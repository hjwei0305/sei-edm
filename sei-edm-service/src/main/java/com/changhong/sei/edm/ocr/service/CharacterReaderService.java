package com.changhong.sei.edm.ocr.service;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.dto.DocumentType;
import com.changhong.sei.edm.dto.OcrType;

/**
 * 实现功能： 字符读取服务
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-03-06 14:14
 */
public interface CharacterReaderService {

    /**
     * 字符读取
     *
     * @param ocrType 识别类型
     * @param data    文件
     * @return 返回读取的内容
     */
    ResultData<String> read(DocumentType docType, OcrType ocrType, byte[] data);
}
