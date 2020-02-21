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
import com.changhong.sei.edm.manager.entity.Thumbnail;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

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
    public ResultData<String> bindBusinessDocuments(String entityId, Collection<String> docIds) {
        //如果文档Id清单为空，不执行操作
        if (Objects.isNull(docIds)) {
            return ResultData.fail("文档Id清单为空");
        }
        //先移除现有业务信息
        List<BusinessDocument> infos = businessDocumentDao.findAllByEntityId(entityId);
        if (Objects.nonNull(infos) && !infos.isEmpty()) {
            businessDocumentDao.deleteAll(infos);
        }
        //插入文档信息
        BusinessDocument bizDoc;
        for (String docId : docIds) {
            bizDoc = new BusinessDocument(entityId, docId);
            businessDocumentDao.save(bizDoc);
        }
        return ResultData.success("ok");
    }

    /**
     * 删除业务实体的文档信息
     *
     * @param entityId 业务实体Id
     */
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

}
