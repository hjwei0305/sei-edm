package com.ecmp.edm.manager;

import com.ecmp.context.ContextUtil;
import com.ecmp.edm.entity.Document;
import com.ecmp.edm.entity.DocumentInfo;
import com.ecmp.enums.UserAuthorityPolicy;
import com.ecmp.enums.UserType;
import com.ecmp.util.FileUtils;
import com.ecmp.util.JsonUtils;
import com.ecmp.vo.LoginStatus;
import com.ecmp.vo.SessionUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
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

    @Before
    public void before() {
        SessionUser sessionUser = new SessionUser();
        sessionUser.setSessionId("sei3-edm");
        sessionUser.setUserId("sei3-edm");
        sessionUser.setAccount("sei3-edm");
        sessionUser.setUserName("sei3-edm");
        sessionUser.setTenantCode("sei3-edm");
        sessionUser.setUserType(UserType.Employee);
        sessionUser.setAuthorityPolicy(UserAuthorityPolicy.GlobalAdmin);
        sessionUser.setLoginTime(new Date());
        sessionUser.setLoginStatus(LoginStatus.success);
        String token = ContextUtil.generateToken(sessionUser);
        sessionUser.setAccessToken(token);
        ContextUtil.setSessionUser(sessionUser);
    }

    @Test
    public void json() {
        String json = "{\"success\":true,\"message\":\"处理成功！\",\"data\":{\"docId\":\"5e9e9ee1ac99c80001a357a2\",\"fileName\":\"demo.pdman.json\",\"documentType\":\"Text\",\"ocrData\":null}}";
        Map map = JsonUtils.fromJson(json, Map.class);
        System.out.println(map);
    }

    @Test
    public void uploadDocument() {
        DocumentInfo info = new DocumentInfo();
        File file = new File("/Users/chaoma/Downloads/自主产品标准化委员会.html");
//        File file = new File("/Users/chaoma/Downloads/00000006.jpg");
        try {
            info.setAppModule("edm");
            info.setFileName("自主产品标准化委员会.html");
            String s = FileUtils.file2Str(file);
            InputStream stream = FileUtils.str2InputStream(s);
            Document document = new Document(info, stream);
            info = manager.uploadDocument(document);
            // 5e9de1179f97c8000916f93f
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(info);
        System.out.println(info.getId());
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
        List<DocumentInfo> infos = manager.getEntityDocumentInfos("BB60035B-8AA5-11EA-8DD4-0242C0A84412");
        System.out.println(infos);
    }
}