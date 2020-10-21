package com.changhong.sei.edm.preview.service.impl;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.file.service.FileConverterService;
import com.changhong.sei.edm.preview.service.PreviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * 实现功能：Office预览服务
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-08 10:38
 */
@Service
public class OfficePreviewServiceImpl implements PreviewService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfPreviewServiceImpl.class);

    @Autowired
    private FileConverterService fileConvertService;

    /**
     * 将文档转为预览文档
     *
     * @param document 需要转换的文件
     * @return 返回预览文档
     */
    @Override
    public ResultData<DocumentResponse> preview(DocumentDto document) {
        if (Objects.isNull(document)) {
            return ResultData.fail("document不能为空.");
        }
        ResultData<DocumentResponse> result;
        try (InputStream inputStream = new ByteArrayInputStream(document.getData())) {
            result = fileConvertService.convertInputStream(inputStream, document.getFileName(), document.getMarkText());
        } catch (Exception e) {
            LOGGER.error("office文档转为预览文档异常", e);
            result = ResultData.fail(document.getFileName() + "-转为预览文档异常");
        }
        return result;
    }

}
