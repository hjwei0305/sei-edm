package com.changhong.sei.edm.preview.service.impl;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.common.util.ImageUtils;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.preview.service.PreviewService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class ImagePreviewServiceImpl implements PreviewService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImagePreviewServiceImpl.class);

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

        if (StringUtils.isNotBlank(document.getMarkText())) {
            try {
                InputStream inputStream = new ByteArrayInputStream(document.getData());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageUtils.pressText(document.getMarkText(), inputStream, outputStream, "黑体", 36, Color.red, 80, 0, 0, 0.3f);
                response.setData(outputStream.toByteArray());

                inputStream.close();
                outputStream.close();
            } catch (Exception e) {
                LOGGER.error("添加水印错误", e);
            }
        }
        return ResultData.success(response);
    }
}
