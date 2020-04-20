package com.changhong.sei.edm.ocr.service.impl;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.test.BaseUnitTest;
import com.changhong.sei.edm.dto.DocumentType;
import com.changhong.sei.edm.dto.OcrType;
import com.changhong.sei.edm.ocr.service.CharacterReaderService;
import com.changhong.sei.util.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-03-06 16:20
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DefaultCharacterReaderServiceImplTest {

    @Autowired
    private CharacterReaderService service;

    @Test
    public void read() {
//        File file1 = new File("D:\\data\\img20190926_16070729.jpg");
//        File file1 = new File("/Users/chaoma/Downloads/123211.png");
        File file1 = new File("/Users/chaoma/Downloads/Image_00114.pdf");

        try {
            byte[] data = FileUtils.readFileToByteArray(file1);

            ResultData<String> resultData = service.read(DocumentType.Pdf, OcrType.Barcode, data);
            System.out.println(resultData);
            System.out.println(resultData.getData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}