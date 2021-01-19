package com.changhong.sei.edm.file.service;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.limiter.support.lock.SeiLock;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.dto.UploadResponse;

import java.io.OutputStream;
import java.util.List;
import java.util.Set;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-03 00:32
 */
public interface FileService {

    /**
     * 上传一个文档(如果是图像生成缩略图)
     *
     * @param dto 文档上传dto
     * @return 文档信息
     */
    ResultData<UploadResponse> uploadDocument(DocumentDto dto);

    /**
     * 合并文件分片
     *
     * @param fileMd5  源整文件md5
     * @param fileName 文件名
     * @return 文档信息
     */
    @Deprecated
    // 待删除 大文件合并可能会出现内存溢出
    ResultData<UploadResponse> mergeFile(String fileMd5, String fileName);

    /**
     * 获取一个文档信息(不含文件内容数据)
     *
     * @param docId 文档Id
     * @return 文档
     */
    DocumentResponse getDocumentInfo(String docId);

    /**
     * 批量获取文档信息(不含文件内容数据)
     *
     * @param docIds 文档
     * @return 文档清单
     */
    List<DocumentResponse> getDocumentInfo(Set<String> docIds);

    /**
     * 获取一个文档(包含信息和数据)
     *
     * @param docId 文档Id
     */
    void getDocumentOutputStream(String docId, boolean hasChunk, OutputStream out);

    /**
     * 获取一个文档(包含信息和数据)
     *
     * @param docId 文档Id
     * @return 文档
     */
    DocumentResponse getDocument(String docId);

    /**
     * 获取缩略图
     *
     * @param docId  文档Id
     * @param width  宽
     * @param height 高
     * @return 返回缩略图
     */
    DocumentResponse getThumbnail(String docId, int width, int height);

    /**
     * 删除文档
     *
     * @param docIds  文档
     * @param isChunk 是否是分块
     * @return 删除结果
     */
    ResultData<String> removeByDocIds(Set<String> docIds, boolean isChunk);

    ResultData<String> removeInvalidDocuments();
}
