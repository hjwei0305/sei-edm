package com.changhong.sei.edm.manager.dao;

import com.changhong.sei.core.dao.BaseEntityDao;
import com.changhong.sei.edm.manager.entity.Document;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-05 16:06
 */
@Repository
public interface DocumentDao extends BaseEntityDao<Document> {

    /**
     * 获取一个时间段的文档信息
     *
     * @param startTime 起始时间
     * @param endTime   截至时间
     * @return 文档信息清单
     */
    List<Document> findAllByUploadedTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
}
