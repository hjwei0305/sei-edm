package com.changhong.sei.edm.ocr.service.impl;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.config.EdmConfigProperties;
import com.changhong.sei.edm.dto.OcrType;
import com.changhong.sei.edm.ocr.service.CharacterReaderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-03-06 14:21
 */
@Component
public class DefaultCharacterReaderServiceImpl implements CharacterReaderService {

    /**
     * 识别条码匹配前缀
     */
    @Value("${sei.edm.ocr.match.prefix:sei}")
    private String matchStr;
    /**
     * tess data 安装目录
     */
    @Value("${sei.edm.ocr.tessdata:none}")
    private String tessDataPath;

    /**
     * 字符读取
     *
     * @param ocrType 识别类型
     * @param data    文件
     * @return 返回读取的内容
     */
    public ResultData<String> read(OcrType ocrType, byte[] data) {
        return null;
    }
}
