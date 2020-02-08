package com.changhong.sei.edm.manager.service;

import com.changhong.sei.core.dao.BaseEntityDao;
import com.changhong.sei.core.service.BaseEntityService;
import com.changhong.sei.edm.manager.dao.DocumentDao;
import com.changhong.sei.edm.manager.entity.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Override
    protected BaseEntityDao<Document> getDao() {
        return dao;
    }


}
