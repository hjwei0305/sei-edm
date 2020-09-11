package com.changhong.sei.edm.manager.service;

import com.changhong.sei.core.dao.BaseEntityDao;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.dto.serach.SearchFilter;
import com.changhong.sei.core.service.BaseEntityService;
import com.changhong.sei.edm.manager.dao.BusinessDocumentDao;
import com.changhong.sei.edm.manager.dao.DocumentDao;
import com.changhong.sei.edm.manager.dao.ThumbnailDao;
import com.changhong.sei.edm.manager.entity.BusinessDocument;
import com.changhong.sei.edm.manager.entity.Document;
import com.changhong.sei.edm.manager.entity.FileChunk;
import com.changhong.sei.edm.manager.entity.Thumbnail;
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
    private ThumbnailDao thumbnailDao;
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
    @Value("${sei.edm.days.clear:180}")
    private int clearDays = 180;
    /**
     * 文档暂存的天数
     */
    @Value("${sei.edm.days.temp:1}")
    private int tempDays = 1;


    /**
     * 提交业务实体的文档信息
     *
     * @param entityId 业务实体Id
     * @param docIds   文档Id清单
     */
    @Transactional
    public ResultData<String> bindBusinessDocuments(String entityId, Collection<String> docIds) {
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
            docIds.stream().filter(Objects::nonNull).forEach(i -> {
                BusinessDocument bizDoc = new BusinessDocument(entityId, i);
                businessDocumentDao.save(bizDoc);
            });
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
        List<BusinessDocument> businessInfos = businessDocumentDao.findAllByEntityId(entityId);
        List<Document> result = new ArrayList<>();
        businessInfos.forEach((i) -> {
            if (StringUtils.isNotBlank(i.getDocId())) {
                Document document = dao.findByProperty(Document.FIELD_DOC_ID, i.getDocId());
                if (Objects.nonNull(document)) {
                    result.add(document);
                }
            }
        });
        return result;
    }

    /**
     * 获取无效文档id(无业务信息的文档)
     */
    public ResultData<Set<String>> getInvalidDocIds() {
        // 当前日期
        LocalDate nowDate = LocalDate.now();
        // 当前日期 - 文档清理的天数
        LocalDateTime startTime = LocalDateTime.of(nowDate.minusDays(clearDays), LocalTime.MIN);
        // 当前日期 - 文档暂存的天数
        LocalDateTime endTime = LocalDateTime.of(nowDate.minusDays(tempDays), LocalTime.MAX);

        //获取可以删除的文档Id清单
        Set<String> docIds = new HashSet<>();
        //获取需要清理的文档Id清单
        List<Document> documents = dao.findAllByUploadedTimeBetween(startTime, endTime);
        if (Objects.nonNull(documents) && !documents.isEmpty()) {
            documents.forEach((d) -> {
                BusinessDocument obj = businessDocumentDao.findFirstByDocId(d.getId());
                if (Objects.isNull(obj)) {
                    docIds.add(d.getId());
                }
            });
        }
        return ResultData.success(docIds);
    }

    /**
     * 删除文档信息
     *
     * @param docIds 文档id集合
     * @return 返回操作结果ø
     */
    public ResultData<String> deleteByDocIds(Set<String> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return ResultData.fail("文档id集合为空.");
        }

        SearchFilter filter;
        // 删除缩略图数据
        filter = new SearchFilter(Thumbnail.FIELD_DOC_ID, docIds, SearchFilter.Operator.IN);
        List<Thumbnail> thumbnails = thumbnailDao.findByFilter(filter);
        if (CollectionUtils.isNotEmpty(thumbnails)) {
            thumbnailDao.deleteInBatch(thumbnails);
        }

        // 删除文档信息
        filter = new SearchFilter(Document.FIELD_DOC_ID, docIds, SearchFilter.Operator.IN);
        List<Document> documents = dao.findByFilter(filter);
        if (CollectionUtils.isNotEmpty(documents)) {
            dao.deleteInBatch(documents);
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
        return dao.findFirstByProperty(Document.FIELD_FILE_MD5, fileMd5);
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
     * 根据文件md5获取文档信息
     *
     * @param fileMd5 文件MD5
     * @return 存在返回文档信息
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
    public void deleteFileChunk(Set<String> ids) {
        if (CollectionUtils.isNotEmpty(ids)) {
            fileChunkService.delete(ids);
        }
    }

}
