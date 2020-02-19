package com.changhong.sei.edm.manager.service;

import com.changhong.sei.core.dao.BaseEntityDao;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.service.BaseEntityService;
import com.changhong.sei.edm.manager.dao.BusinessDocumentDao;
import com.changhong.sei.edm.manager.dao.DocumentDao;
import com.changhong.sei.edm.manager.entity.BusinessDocument;
import com.changhong.sei.edm.manager.entity.Document;
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
public class BusinessDocumentService extends BaseEntityService<BusinessDocument> {

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

    @Autowired
    private DocumentDao documentDao;
    @Autowired
    private BusinessDocumentDao dao;

    @Override
    protected BaseEntityDao<BusinessDocument> getDao() {
        return dao;
    }


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
        List<BusinessDocument> infos = dao.findAllByEntityId(entityId);
        if (Objects.nonNull(infos) && !infos.isEmpty()) {
            dao.deleteAll(infos);
        }
        //插入文档信息
        BusinessDocument bizDoc;
        for (String docId : docIds) {
            bizDoc = new BusinessDocument(entityId, docId);
            dao.save(bizDoc);
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
        List<BusinessDocument> businessInfos = dao.findAllByEntityId(entityId);
        List<Document> result = new ArrayList<>();
        businessInfos.forEach((i) -> {
            if (StringUtils.isNotBlank(i.getDocId())) {
                Document document = documentDao.findByProperty(Document.FIELD_DOC_ID, i.getDocId());
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
        List<Document> documents = documentDao.findAllByUploadedTimeBetween(startTime, endTime);
        if (Objects.nonNull(documents) && !documents.isEmpty()) {
            documents.forEach((d) -> {
                BusinessDocument obj = dao.findFirstByDocId(d.getId());
                if (Objects.isNull(obj)) {
                    docIds.add(d.getId());
                }
            });
        }
        return ResultData.success(docIds);
    }
}
