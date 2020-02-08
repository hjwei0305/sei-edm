package com.changhong.sei.edm.preview.service;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;

/**
 * 实现功能：文件预览服务接口
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-08 10:38
 */
public interface PreviewService {

    /**
     * 将文档转为预览文档
     *
     * @param document 需要转换的文件
     * @return 返回预览文档
     */
    ResultData<DocumentResponse> preview(DocumentDto document);
}
