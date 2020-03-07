package com.changhong.sei.edm.preview.service.impl;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.common.util.PdfUtils;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.preview.service.PreviewService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * 实现功能：pdf预览服务
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-08 10:38
 */
@Service
public class PdfPreviewServiceImpl implements PreviewService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfPreviewServiceImpl.class);

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
                // 水印
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                PdfUtils.watermarkPDF(document.getData(), document.getMarkText(), outputStream);

                response.setData(outputStream.toByteArray());
                response.setSize((long) outputStream.size());

                outputStream.close();
            } catch (Exception e) {
                LOGGER.error("添加水印错误", e);
            }
        }
        return ResultData.success(response);
    }
}
