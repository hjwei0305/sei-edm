package com.changhong.sei.edm.manager.service;

import com.changhong.sei.core.dao.BaseEntityDao;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.dto.serach.SearchFilter;
import com.changhong.sei.core.service.BaseEntityService;
import com.changhong.sei.edm.common.util.DocumentTypeUtil;
import com.changhong.sei.edm.manager.dao.BusinessDocumentDao;
import com.changhong.sei.edm.manager.dao.DocumentDao;
import com.changhong.sei.edm.manager.entity.BusinessDocument;
import com.changhong.sei.edm.manager.entity.Document;
import com.changhong.sei.edm.manager.entity.FileChunk;
import com.changhong.sei.util.IdGenerator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-05 16:10
 */
@Service
public class DocumentService extends BaseEntityService<Document> {

    @Autowired
    private DocumentDao dao;
    @Autowired
    private BusinessDocumentDao businessDocumentDao;
    @Autowired
    private FileChunkService fileChunkService;

    @Override
    protected BaseEntityDao<Document> getDao() {
        return dao;
    }

    /**
     * 文档清理的天数
     */
//    @Value("${sei.edm.days.clear:180}")
//    private int clearDays = 180;
    /**
     * 文档暂存的天数
     */
    @Value("${sei.edm.days.temp:5}")
    private int tempDays = 1;


    /**
     * 提交业务实体的文档信息
     *
     * @param entityId 业务实体Id
     * @param docIds   文档Id清单
     */
    @Transactional
    public ResultData<String> bindBusinessDocuments(String entityId, Collection<String> docIds) {
        //如果entityId为空，不执行操作
        if (StringUtils.isBlank(entityId)) {
            return ResultData.fail("entityId为空");
        }
        //如果文档Id清单为空，不执行操作
        if (Objects.isNull(docIds)) {
            return ResultData.fail("文档Id清单为空");
        }
        List<BusinessDocument> infos = businessDocumentDao.findAllByEntityId(entityId);
        if (CollectionUtils.isNotEmpty(infos)) {
            List<String> existDocId = infos.parallelStream().map(BusinessDocument::getDocId).collect(Collectors.toList());
            //删除需要删除的部分
            List<BusinessDocument> deleteInfos = infos.parallelStream().filter(i -> !docIds.contains(i.getDocId())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(deleteInfos)) {
                businessDocumentDao.deleteAll(deleteInfos);
            }
            //插入部分移除已存在部分
            docIds.removeAll(existDocId);
        }
        //插入文档信息
        if (CollectionUtils.isNotEmpty(docIds)) {
            List<BusinessDocument> businessDocuments = new ArrayList<>();
            docIds.stream().filter(Objects::nonNull).forEach(i -> {
                BusinessDocument bizDoc = new BusinessDocument(entityId, i);
                businessDocuments.add(bizDoc);
            });
            businessDocumentDao.save(businessDocuments);
        }
        return ResultData.success("ok");
    }

    /**
     * 删除业务实体的文档信息
     *
     * @param entityId 业务实体Id
     */
    @Transactional
    public ResultData<String> unbindBusinessDocuments(String entityId) {
        return bindBusinessDocuments(entityId, new ArrayList<>());
    }

    /**
     * 获取业务实体的文档信息清单
     *
     * @param entityId 业务实体Id
     * @return 文档信息清单
     */
    public List<Document> getDocumentsByEntityId(String entityId) {
        List<Document> result = null;
        List<BusinessDocument> businessInfos = businessDocumentDao.findAllByEntityId(entityId);
        if (Objects.nonNull(businessInfos)) {
            Set<String> docIds = businessInfos.stream().map(BusinessDocument::getDocId).collect(Collectors.toSet());
            if (docIds.size() > 0) {
                result = this.getDocs(docIds);
            }
        }
        if (Objects.isNull(result)) {
            result = new ArrayList<>();
        }
        return result;
    }

    // 分组大小
    private static final int MAX_NUMBER = 500;

    /**
     * 获取无效文档id(无业务信息的文档)
     * 从所有过存储有效期的docId(包含关联业务与未关联业务的)清单中排除有关联业务的docId(有业务关联的docId不能删除)
     */
    public ResultData<Set<String>> getInvalidDocIds() {
        // 当前日期
        LocalDate nowDate = LocalDate.now();
        // 当前日期 - 文档清理的天数
//        LocalDateTime startTime = LocalDateTime.of(nowDate.minusDays(clearDays), LocalTime.MIN);
        // 当前日期 - 文档暂存的天数
        LocalDateTime endTime = LocalDateTime.of(nowDate.minusDays(tempDays), LocalTime.MAX);

        //获取可以删除的文档Id清单
        Set<String> expiredAndNonBizDocIds;
        //获取需要清理的文档Id清单
        List<Document> documents = dao.findAllByUploadedTimeLessThanEqual(endTime);
        if (Objects.nonNull(documents) && !documents.isEmpty()) {
            // 所有过存储有效期的docId(包含关联业务与未关联业务的)
            Set<String> expiredDocIds = documents.stream()
                    // 没有文件分块的
                    .filter(d -> !d.getHasChunk())
                    .map(Document::getDocId).collect(Collectors.toSet());
            documents.clear();

            // 分组处理,防止数据太多导致异常(in查询限制)
            // 计算组数
            int limit = (expiredDocIds.size() + MAX_NUMBER - 1) / MAX_NUMBER;
            //方法一：使用流遍历操作
            List<Set<String>> mglist = new ArrayList<>();
            Stream.iterate(0, n -> n + 1).limit(limit).forEach(i -> {
                mglist.add(expiredDocIds.stream().skip(i * MAX_NUMBER).limit(MAX_NUMBER).collect(Collectors.toSet()));
            });

            // 存在业务关联的docIds
            Set<String> bizDocIds = new HashSet<>();
            List<BusinessDocument> bizDocs;
            for (Set<String> set : mglist) {
                bizDocs = businessDocumentDao.findByDocIdIn(set);
                if (CollectionUtils.isNotEmpty(bizDocs)) {
                    bizDocIds.addAll(bizDocs.stream().map(BusinessDocument::getDocId).collect(Collectors.toSet()));
                }
                bizDocs.clear();
            }

            // 从所有过存储有效期的docId(包含关联业务与未关联业务的)清单中排除有关联业务的docId(有业务关联的docId不能删除)
            expiredAndNonBizDocIds = expiredDocIds.stream().filter(i -> !bizDocIds.contains(i)).collect(Collectors.toSet());
        } else {
            expiredAndNonBizDocIds = new HashSet<>();
        }
        return ResultData.success(expiredAndNonBizDocIds);
    }

    /**
     * 获取无效分块的文档id(无业务信息的文档)
     * 从所有过存储有效期的docId(包含关联业务与未关联业务的)清单中排除有关联业务的docId(有业务关联的docId不能删除)
     */
    public ResultData<Set<String>> getInvalidChunkDocIds() {
        // 当前日期
        LocalDate nowDate = LocalDate.now();
        // 当前日期 - 文档清理的天数
//        LocalDateTime startTime = LocalDateTime.of(nowDate.minusDays(clearDays), LocalTime.MIN);
        // 当前日期 - 文档暂存的天数
        LocalDateTime endTime = LocalDateTime.of(nowDate.minusDays(tempDays), LocalTime.MIN);

        //获取可以删除的文档Id清单
        Set<String> expiredAndNonBizDocIds;
        //获取需要清理的分块文档Id清单
        List<FileChunk> chunkList = fileChunkService.findAllByUploadedTimeLessThanEqual(endTime);
        if (Objects.nonNull(chunkList) && !chunkList.isEmpty()) {

            // 所有过存储有效期的原大文件docId
            Set<String> expiredOriginDocIds = chunkList.stream()
                    // 没有关联原大文件的所有临时分块
                    .filter(d -> StringUtils.isNotBlank(d.getOriginDocId()))
                    .map(FileChunk::getOriginDocId).collect(Collectors.toSet());

            // 分组处理,防止数据太多导致异常(in查询限制)
            // 计算组数
            int limit = (expiredOriginDocIds.size() + MAX_NUMBER - 1) / MAX_NUMBER;
            //方法一：使用流遍历操作
            List<Set<String>> mglist = new ArrayList<>();
            Stream.iterate(0, n -> n + 1).limit(limit).forEach(i -> {
                mglist.add(expiredOriginDocIds.stream().skip(i * MAX_NUMBER).limit(MAX_NUMBER).collect(Collectors.toSet()));
            });

            // 存在业务关联的docIds
            Set<String> bizDocIds = new HashSet<>();
            List<BusinessDocument> bizDocs;
            for (Set<String> set : mglist) {
                // 每次最大 MAX_NUMBER 个
                bizDocs = businessDocumentDao.findByDocIdIn(set);
                if (CollectionUtils.isNotEmpty(bizDocs)) {
                    bizDocIds.addAll(bizDocs.stream().map(BusinessDocument::getDocId).collect(Collectors.toSet()));
                }
                bizDocs.clear();
            }
            // 从所有过存储有效期的docId(包含关联业务与未关联业务的)清单中排除有关联业务的docId(有业务关联的docId不能删除)
            Set<String> expiredAndNonBizOriginDocIds = expiredOriginDocIds.stream().filter(i -> !bizDocIds.contains(i)).collect(Collectors.toSet());

            // 所有过存储有效期的docId(没有关联原大文件的所有临时分块)
            expiredAndNonBizDocIds = chunkList.stream()
                    // 没有关联原大文件的所有临时分块 或者 原文件docId未关联业务的
                    .filter(d -> StringUtils.isBlank(d.getOriginDocId()) || expiredAndNonBizOriginDocIds.contains(d.getOriginDocId()))
                    .map(FileChunk::getDocId).collect(Collectors.toSet());

            chunkList.clear();
        } else {
            expiredAndNonBizDocIds = new HashSet<>();
        }
        return ResultData.success(expiredAndNonBizDocIds);
    }

    /**
     * 删除文档信息
     *
     * @param docIds 文档id集合
     * @return 返回操作结果ø
     */
    @Transactional
    public ResultData<String> deleteByDocIds(Set<String> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return ResultData.fail("文档id集合为空.");
        }

        // 分组处理,防止数据太多导致异常(in查询限制)
        // 计算组数
        int limit = (docIds.size() + MAX_NUMBER - 1) / MAX_NUMBER;
        //方法一：使用流遍历操作
        List<Set<String>> mglist = new ArrayList<>();
        Stream.iterate(0, n -> n + 1).limit(limit).forEach(i -> {
            mglist.add(docIds.stream().skip(i * MAX_NUMBER).limit(MAX_NUMBER).collect(Collectors.toSet()));
        });

        List<Document> documents;
        for (Set<String> tempDocIds : mglist) {
            // 删除文档信息
            documents = this.getDocs(tempDocIds);
            if (CollectionUtils.isNotEmpty(documents)) {
                dao.deleteInBatch(documents);
            }
        }
        return ResultData.success("OK");
    }

    /**
     * 根据文件md5获取文档信息
     *
     * @param fileMd5 文件md5
     * @return 存在返回文档信息
     */
    public Document getDocumentByMd5(String fileMd5) {
        if (StringUtils.isNotBlank(fileMd5)) {
            return dao.findFirstByProperty(Document.FIELD_FILE_MD5, fileMd5);
        } else {
            return null;
        }
    }

    /**
     * 按docId获取文档
     *
     * @param docId
     * @return
     */
    public Document getByDocId(String docId) {
        if (StringUtils.isNotBlank(docId)) {
            return dao.findFirstByIdOrDocId(docId, docId);
        } else {
            return null;
        }
    }

    /**
     * 按docId获取文档
     *
     * @param docIds
     * @return
     */
    public List<Document> getDocs(Collection<String> docIds) {
        if (CollectionUtils.isNotEmpty(docIds)) {
            List<Document> documents = dao.findDocs(docIds, docIds);
            // 历史数据原因,增加过渡阶段补充代码 start    计划在2021-06-30移除该部分过渡代码
            if (CollectionUtils.isNotEmpty(documents)) {
                int index = 0;
                int size = docIds.size();
                List<Document> result = new ArrayList<>(docIds.size());
                for (Document document : documents) {
                    if ((docIds.contains(document.getDocId()) || docIds.contains(document.getId())) && size > index) {
                        index++;
                        result.add(document);
                    }
                }
                return result;
            }
            // 历史数据原因,增加过渡阶段补充代码 end
        }
        return null;
    }

    /**
     * 根据文件md5获取文档信息
     *
     * @param chunk    文件块
     * @param docId    文件块docId
     * @param fileName 文件名
     * @return 存在返回文档信息
     */
    public FileChunk saveFileChunk(FileChunk chunk, String docId, String fileName) {
        if (Objects.nonNull(chunk)) {
            chunk.setDocId(docId);
            chunk.setFilename(fileName);
            chunk.setUploadedTime(LocalDateTime.now());
            fileChunkService.save(chunk);
        }
        return chunk;
    }

    /**
     * 根据关联的原大文件docId获取文档分块信息
     *
     * @param originDocId 关联的原大文件docId
     * @return 存在返回顺序的分块清单
     */
    public List<FileChunk> getFileChunkByOriginDocId(String originDocId) {
        List<FileChunk> chunks;
        if (StringUtils.isNotBlank(originDocId)) {
            chunks = fileChunkService.findListByProperty(FileChunk.FIELD_ORIGIN_DOC_ID, originDocId);
            // 按文件块序号排序
            chunks.sort(Comparator.comparingInt(FileChunk::getChunkNumber));
        } else {
            chunks = new ArrayList<>();
        }
        return chunks;
    }

    /**
     * 根据文件md5获取文档信息
     *
     * @param fileMd5 文件MD5
     * @return 存在返回顺序的分块清单
     */
    public List<FileChunk> getFileChunk(String fileMd5) {
        List<FileChunk> chunks;
        if (StringUtils.isNotBlank(fileMd5)) {
            chunks = fileChunkService.findListByProperty(FileChunk.FIELD_FILE_MD5, fileMd5);
            // 按文件块序号排序
            chunks.sort(Comparator.comparingInt(FileChunk::getChunkNumber));
        } else {
            chunks = new ArrayList<>();
        }
        return chunks;
    }

    /**
     * 删除分片文件数据
     *
     * @param ids 分片文件id
     */
    @Transactional
    public void deleteFileChunk(Set<String> ids) {
        if (CollectionUtils.isNotEmpty(ids)) {
            fileChunkService.delete(ids);
        }
    }

    @Transactional
    public void deleteChunkByDocIdIn(Set<String> docIds) {
        if (CollectionUtils.isNotEmpty(docIds)) {
            fileChunkService.deleteByDocIdIn(docIds);
        }
    }

    /**
     * 合并文件分片
     *
     * @param fileMd5  源整文件md5
     * @param fileName 文件名
     * @return 文档docId
     */
    @Transactional
    public ResultData<String> mergeFile(String fileMd5, String fileName) {
        List<FileChunk> chunks = this.getFileChunk(fileMd5);
        if (CollectionUtils.isNotEmpty(chunks)) {
            String docId = IdGenerator.uuid2();
            long totalSize = 0;
            for (FileChunk chunk : chunks) {
                chunk.setOriginDocId(docId);
                totalSize = chunk.getTotalSize();
            }

            fileChunkService.save(chunks);

            Document document = new Document(fileName);
            document.setDocId(docId);
            document.setFileMd5(fileMd5);
            // 标示有分块
            document.setHasChunk(Boolean.TRUE);
            document.setSize(totalSize);
            document.setUploadedTime(LocalDateTime.now());
            document.setDocumentType(DocumentTypeUtil.getDocumentType(fileName));

            this.save(document);

            return ResultData.success(docId);
        } else {
            return ResultData.fail("文件分片不存在.");
        }
    }
}
