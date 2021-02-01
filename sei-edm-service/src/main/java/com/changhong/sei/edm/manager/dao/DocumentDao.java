package com.changhong.sei.edm.manager.dao;

import com.changhong.sei.core.dao.BaseEntityDao;
import com.changhong.sei.edm.manager.entity.Document;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-05 16:06
 */
@Repository
public interface DocumentDao extends BaseEntityDao<Document> {

    @Query(value = "select d from Document d where d.id in :ids or d.docId in :docIds")
    List<Document> findDocs(@Param("ids") Collection<String> ids,
                           @Param("docIds") Collection<String> docIds);

    Document findFirstByIdOrDocId(String id, String docId);

    /**
     * 获取一个时间段的文档信息
     *
     * @param startTime 起始时间
     * @param endTime   截至时间
     * @return 文档信息清单
     */
    List<Document> findAllByUploadedTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<Document> findAllByUploadedTimeLessThanEqual(LocalDateTime endTime);
}
