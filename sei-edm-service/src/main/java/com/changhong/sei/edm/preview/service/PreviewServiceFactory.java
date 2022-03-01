package com.changhong.sei.edm.preview.service;

import com.changhong.sei.core.context.ApplicationContextHolder;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.preview.service.impl.*;
import com.changhong.sei.exception.ServiceException;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 实现功能：预览服务工厂类
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-08 10:38
 */
public final class PreviewServiceFactory {

    private static Map<String, PreviewService> SERVICE_MAP = null;

    /**
     * 文件预览转换
     *
     * @param document 需要转换的文件
     * @return 返回预览文档
     */
    public static ResultData<DocumentResponse> getPreviewDocument(DocumentDto document) {
        if (SERVICE_MAP == null) {
            SERVICE_MAP = ApplicationContextHolder.getApplicationContext().getBeansOfType(PreviewService.class);
        }

        PreviewService previewService;
        switch (document.getDocumentType()) {
            case Pdf:
            case OFD:
                previewService = SERVICE_MAP.get(getPreviewServiceName(PdfPreviewServiceImpl.class));
                break;
            case Word:
            case Excel:
            case Powerpoint:
                previewService = SERVICE_MAP.get(getPreviewServiceName(OfficePreviewServiceImpl.class));
                break;
            case Image:
                previewService = SERVICE_MAP.get(getPreviewServiceName(ImagePreviewServiceImpl.class));
                break;
            case Media:
                previewService = SERVICE_MAP.get(getPreviewServiceName(MediaPreviewServiceImpl.class));
                break;
            case Text:
                previewService = SERVICE_MAP.get(getPreviewServiceName(TextPreviewServiceImpl.class));
                break;
            case Compressed:
                previewService = SERVICE_MAP.get(getPreviewServiceName(CompressedPreviewServiceImpl.class));
                break;
            case Other:
            default:
                throw new ServiceException("不支持的文件预览类型");
        }

        return previewService.preview(document);
    }

    /**
     * 获取预览实现类名
     */
    private static String getPreviewServiceName(Class<?> clazz) {
        return StringUtils.uncapitalize(clazz.getSimpleName());
    }
}
