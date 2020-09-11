package com.changhong.sei.edm.file.service.convert;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.file.service.FileConverterService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-01 10:55
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class FileConverterServiceImplTest {

    @Autowired
    private FileConverterService service;

    @Test
    public void testConvertFile() {
        File sourceFile = new File("/Users/chaoma/Downloads/1工资及保险新版-082012.xlsx");
        String targetFileDir = "/Users/chaoma/Downloads/";
        ResultData<String> resultData = service.convertFile(sourceFile, targetFileDir);
        System.out.println(resultData);
    }
}