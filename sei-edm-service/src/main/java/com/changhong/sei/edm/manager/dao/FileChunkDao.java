package com.changhong.sei.edm.manager.dao;

import com.changhong.sei.core.dao.BaseEntityDao;
import com.changhong.sei.edm.manager.entity.FileChunk;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-05 16:06
 */
@Repository
public interface FileChunkDao extends BaseEntityDao<FileChunk> {

    @Modifying
    int deleteByDocIdIn(Set<String> docIds);

    /**
     * 获取一个时间段的文档信息
     *
     * @param startTime 起始时间
     * @param endTime   截至时间
     * @return 文档信息清单
     */
    List<FileChunk> findAllByUploadedTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
}
