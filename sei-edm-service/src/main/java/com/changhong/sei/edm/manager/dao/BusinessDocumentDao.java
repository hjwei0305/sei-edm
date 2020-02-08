package com.changhong.sei.edm.manager.dao;

import com.changhong.sei.core.dao.BaseEntityDao;
import com.changhong.sei.edm.manager.entity.BusinessDocument;
import com.changhong.sei.edm.manager.entity.Document;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-05 16:06
 */
@Repository
public interface BusinessDocumentDao extends BaseEntityDao<BusinessDocument> {
    /**
     * 通过业务实体Id获取业务信息
     *
     * @param entityId 业务实体Id
     * @return 业务信息清单
     */
    List<BusinessDocument> findAllByEntityId(String entityId);

    /**
     * 判断文档Id是否存在业务信息
     *
     * @param docId 文档Id
     * @return 业务信息
     */
    BusinessDocument findFirstByDocId(String docId);
}
