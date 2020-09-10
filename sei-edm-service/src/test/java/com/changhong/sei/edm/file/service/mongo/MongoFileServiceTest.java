package com.changhong.sei.edm.file.service.mongo;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.dto.UploadResponse;
import com.changhong.sei.edm.manager.entity.FileChunk;
import com.changhong.sei.edm.manager.service.FileChunkService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-10 09:35
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class MongoFileServiceTest {

    @Autowired
    private MongoFileService service;

    @Autowired
    private FileChunkService fileChunkService;

    @Test
    public void testMergeFile() {
        String fileMd5 = "aebf97290b30e0e98954ea327eb87154";

        ResultData<UploadResponse> responseResultData = service.mergeFile(fileMd5, "easy-ui-design.zip");
        System.out.println(responseResultData);

        try {
            Thread.sleep(2 * 60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getDocument() {
        String docId = "5f598af8896d3cd3565127dd";

        DocumentResponse response = service.getDocument(docId);
        System.out.println(response);

        FileOutputStream fos = null;
        ByteArrayInputStream in = null;

        byte[] buf = new byte[1024];
        int len = 0;
        try {
            in = new ByteArrayInputStream(response.getData());
            fos = new FileOutputStream(new File("/Users/chaoma/Downloads/easy-ui-design.zip"));
            while ((len = in.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fos.close();
            in.close();
        } catch (IOException e) {
        }
    }

    @Test
    public void delete() {
        List<FileChunk> chunkList = fileChunkService.findAll();
        Set<String> ids = chunkList.stream().map(FileChunk::getId).collect(Collectors.toSet());
        Set<String> docIds = chunkList.stream().map(FileChunk::getDocId).collect(Collectors.toSet());

        service.removeByDocIds(docIds);
        fileChunkService.delete(ids);
    }
}