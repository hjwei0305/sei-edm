package com.changhong.sei.edm.manager.service;

import com.changhong.sei.core.dao.BaseEntityDao;
import com.changhong.sei.core.service.BaseEntityService;
import com.changhong.sei.edm.manager.dao.FileChunkDao;
import com.changhong.sei.edm.manager.entity.FileChunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-05 16:10
 */
@Service
public class FileChunkService extends BaseEntityService<FileChunk> {

    @Autowired
    private FileChunkDao dao;

    @Override
    protected BaseEntityDao<FileChunk> getDao() {
        return dao;
    }

    public int deleteByDocIdIn(Set<String> docIds) {
        return dao.deleteByDocIdIn(docIds);
    }

    /**
     * 获取一个时间段的文档信息
     *
     * @param startTime 起始时间
     * @param endTime   截至时间
     * @return 文档信息清单
     */
    public List<FileChunk> findAllByUploadedTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return dao.findAllByUploadedTimeBetween(startTime, endTime);
    }

}
