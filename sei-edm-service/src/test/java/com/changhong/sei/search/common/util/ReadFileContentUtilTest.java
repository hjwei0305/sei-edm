package com.changhong.sei.search.common.util;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.junit.Assert.*;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 16:32
 */
public class ReadFileContentUtilTest {

    @Test
    public void readDocxContent() {
        File file = new File("/Users/chaoma/Downloads/长虹多媒体（河边工业园）WMS项目系统规格说明书V2.9.6 - 智能智造--仓库.docx");
        try {
            String s = ReadFileContentUtil.readDocxContent(new FileInputStream(file));
            System.out.println(s);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}