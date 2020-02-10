package com.changhong.sei.edm.common.constant;

import com.changhong.sei.edm.dto.DocumentType;

import java.util.HashMap;
import java.util.Map;

/**
 * 实现功能：
 * 常量定义
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-07 13:41
 */
public final class Constants {

    public static final Map<DocumentType, String> DOC_TYPE_MAP;

    static {
        DOC_TYPE_MAP = new HashMap<>();
        // 图片
        DOC_TYPE_MAP.put(DocumentType.Image, "jpg|bmp|gif|png|jpeg");
        // pdf
        DOC_TYPE_MAP.put(DocumentType.Pdf, "pdf");
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
        DOC_TYPE_MAP.put(DocumentType.Text, "txt|html|htm|asp|jsp|xml|json|properties|md|gitignore,java|py|c|cpp|sql|sh|bat|m|bas|prg|cmd");
    }

    private Constants() {
    }
}
