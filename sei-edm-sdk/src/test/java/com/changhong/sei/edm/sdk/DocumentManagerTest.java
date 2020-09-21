package com.changhong.sei.edm.sdk;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.dto.UploadResponse;
import com.changhong.sei.util.FileUtils;
import org.apache.curator.shaded.com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-04-20 23:22
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DocumentManagerTest {

    @Autowired
    private DocumentManager manager;

    @Test
    public void uploadChunk() {
        String fileName = "归档.zip";
        File file1 = new File("/Users/chaoma/Downloads/归档.zip");
        try (InputStream stream = FileUtils.openInputStream(file1)) {
            ResultData<UploadResponse> resultData = manager.uploadChunk(fileName, stream);
            System.out.println(resultData);

            Thread.sleep(300 * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void uploadDocument() {
        File file1 = new File("/Users/chaoma/Downloads/demo.pdman.json");

        byte[] data = new byte[0];
        try {
            data = FileUtils.readFileToByteArray(file1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        UploadResponse response = manager.uploadDocument("demo.pdman.json", data);
        System.out.println(response);
        // 5e9dde3b9f97c8000916f93d
    }

    @Test
    public void testUploadDocument() {
        File file1 = new File("/Users/chaoma/Downloads/demo.pdman.json");

        try {
            InputStream data = FileUtils.openInputStream(file1);
            UploadResponse response = manager.uploadDocument("demo.pdman.json", data);
            System.out.println(response);
            // 5e9de1179f97c8000916f93f
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void uploadScan() {
    }

    @Test
    public void getDocument() {
        String docId = "5e9dde3b9f97c8000916f93d";
        DocumentResponse response = manager.getDocument(docId, false);
        if (Objects.nonNull(response)) {

            try {
                //InputStream inputStream = FileUtils.str2InputStream(response.getBase64Data());
                FileUtils.str2File(response.getBase64Data(), "/Users/chaoma/Downloads/demo.pdman123.json");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println(response);
    }

    @Test
    public void bindBusinessDocuments() {
        ResultData<String> resultData = manager.bindBusinessDocuments("947B28E7-7E0F-11EA-96B4-0242C0A84405", Lists.newArrayList("5e9dde3b9f97c8000916f93d"));
        System.out.println(resultData);
    }

    @Test
    public void deleteBusinessInfos() {
        ResultData<String> resultData = manager.deleteBusinessInfos("947B28E7-7E0F-11EA-96B4-0242C0A84405");
        System.out.println(resultData);
    }

    @Test
    public void getEntityDocumentInfo() {
        ResultData<DocumentResponse> resultData = manager.getEntityDocumentInfo("5e9dde3b9f97c8000916f93d");
        System.out.println(resultData);
    }

    @Test
    public void getEntityDocumentInfos() {
        ResultData<List<DocumentResponse>> resultData = manager.getEntityDocumentInfos("947B28E7-7E0F-11EA-96B4-0242C0A84405");
        System.out.println(resultData);
    }
}