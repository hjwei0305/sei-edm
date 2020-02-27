package com.changhong.sei.edm.file.service;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.dto.DocumentResponse;

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
     * @param fileName 文件名
     * @param sys      来源系统
     * @param data     文档数据
     * @return 文档信息
     */
    ResultData<String> uploadDocument(String fileName, String sys, byte[] data);

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
     * @param docIds 文档
     * @return 删除结果
     */
    ResultData<String> removeByDocIds(Set<String> docIds);

    ResultData<String> removeInvalidDocuments();
}
