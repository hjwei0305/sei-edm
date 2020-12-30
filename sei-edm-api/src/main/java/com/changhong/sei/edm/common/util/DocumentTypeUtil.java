package com.changhong.sei.edm.common.util;

import com.changhong.sei.edm.dto.DocumentType;
import com.changhong.sei.util.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 实现功能：
 * 常量定义
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-07 13:41
 */
public final class DocumentTypeUtil {

    public static final Map<DocumentType, String> DOC_TYPE_MAP;

    static {
        DOC_TYPE_MAP = new HashMap<>();
        // 图片
        DOC_TYPE_MAP.put(DocumentType.Image, "jpg|bmp|gif|png|jpeg");
        // pdf
        DOC_TYPE_MAP.put(DocumentType.Pdf, "pdf");
        // ofd
        DOC_TYPE_MAP.put(DocumentType.OFD, "ofd");
        // word
        DOC_TYPE_MAP.put(DocumentType.Word, "doc|docx");
        // excel
        DOC_TYPE_MAP.put(DocumentType.Excel, "xls|xlsx|csv");
        // ppt
        DOC_TYPE_MAP.put(DocumentType.Powerpoint, "ppt|pptx");
        // 压缩文件
        DOC_TYPE_MAP.put(DocumentType.Compressed, "zip|rar|7z");
        // 多媒体文件
        DOC_TYPE_MAP.put(DocumentType.Media, "mp3|wav|mp4|flv|avi");
        // 文本文件
        DOC_TYPE_MAP.put(DocumentType.Text, "txt|xml|json|properties|md|java|sql");
//        DOC_TYPE_MAP.put(DocumentType.Text, "txt|html|htm|asp|jsp|xml|json|properties|md|gitignore|java|py|c|cpp|sql|sh|bat|m|bas|prg|cmd");
    }

    private DocumentTypeUtil() {
    }

    /**
     * 通过文件名获取文档类型
     *
     * @param fileName 文件名
     * @return 文档类型
     */
    public static DocumentType getDocumentType(String fileName) {
        String extension = FileUtils.getExtension(fileName);
        if (StringUtils.isBlank(extension)) {
            return DocumentType.Other;
        }
        extension = extension.toLowerCase();
        for (Map.Entry<DocumentType, String> entry : DocumentTypeUtil.DOC_TYPE_MAP.entrySet()) {
            if (StringUtils.contains(entry.getValue(), extension)) {
                return entry.getKey();
            }
        }
        return DocumentType.Other;
    }
}
