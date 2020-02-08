package com.changhong.sei.edm.preview.service.impl;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.preview.service.PreviewService;
import org.springframework.stereotype.Service;

@Service
public class CompressedPreviewServiceImpl implements PreviewService {

    /**
     * 将文档转为预览文档
     *
     * @param document 需要转换的文件
     * @return 返回预览文档
     */
    @Override
    public ResultData<DocumentResponse> preview(DocumentDto document) {
        DocumentResponse response = new DocumentResponse();
        // 注意: 仅用于URLConnection.guessContentTypeFromName获取MimeType,无其他意义
        response.setFileName(document.getFileName());
        response.setData(document.getData());
        response.setSize(document.getSize());
        return ResultData.success(response);
    }
}
