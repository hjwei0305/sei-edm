package com.ecmp.edm.manager;

import com.ecmp.edm.entity.Document;
import com.ecmp.edm.entity.DocumentInfo;
import com.ecmp.util.FileUtils;
import com.ecmp.util.JsonUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-04-21 15:02
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DocumentManagerTest {
    @Autowired
    private IDocumentManager manager;

    @Test
    public void json() {
        String json = "{\"success\":true,\"message\":\"处理成功！\",\"data\":{\"docId\":\"5e9e9ee1ac99c80001a357a2\",\"fileName\":\"demo.pdman.json\",\"documentType\":\"Text\",\"ocrData\":null}}";
        Map map = JsonUtils.fromJson(json, Map.class);
        System.out.println(map);
    }

    @Test
    public void uploadDocument() {
        DocumentInfo info = new DocumentInfo();
        File file = new File("/Users/chaoma/Downloads/demo.pdman.json");
        try {
            info.setAppModule("test");
            info.setFileName("demo.pdman.json");
            String s = FileUtils.file2Str(file);
            InputStream stream = FileUtils.str2InputStream(s);
            Document document = new Document(info, stream);
            info = manager.uploadDocument(document);
            // 5e9de1179f97c8000916f93f
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(info);
    }

    @Test
    public void testUploadDocument() {
        File file = new File("/Users/chaoma/Downloads/demo.pdman.json");
        try {
            String s = FileUtils.file2Str(file);
            InputStream stream = FileUtils.str2InputStream(s);
            DocumentInfo info = manager.uploadDocument(stream, "demo.pdman.json");
            // 5e9de1179f97c8000916f93f    5e9ea1e7ac99c80001a357a4
            System.out.println(info);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getDocument() {
        Document document = manager.getDocument("5e9de1179f97c8000916f93f", false);
        System.out.println(document);
    }

    @Test
    public void submitBusinessInfos() {
        List<String> list = new ArrayList<>();
        list.add("5e9de1179f97c8000916f93f");
        manager.submitBusinessInfos("TEST_123456", list);
    }

    @Test
    public void deleteBusinessInfos() {
        manager.deleteBusinessInfos("TEST_123456");
    }

    @Test
    public void getEntityDocumentInfos() {
        List<DocumentInfo> infos = manager.getEntityDocumentInfos("TEST_123456");
        System.out.println(infos);
    }
}