package com.changhong.sei.edm.manager.service;

import com.changhong.sei.core.test.BaseUnitTest;
import com.changhong.sei.edm.manager.entity.FileChunk;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-11 08:44
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DocumentServiceTest {

    @Autowired
    private DocumentService service;

    @Test
    public void getFileChunk() {
        String md5 = "172c82eb224c92e76a328deb788772f6";
        List<FileChunk> list = service.getFileChunk(md5);
        System.out.println(list);
    }
}