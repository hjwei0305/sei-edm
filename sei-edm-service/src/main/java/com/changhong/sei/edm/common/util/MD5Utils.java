package com.changhong.sei.edm.common.util;

import org.apache.commons.lang3.time.StopWatch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-09 14:59
 */
public class MD5Utils {
    private static MessageDigest md;

    static {
        try {
            //初始化摘要对象
            md = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println(md5("test"));
        StopWatch stopWatch = StopWatch.createStarted();
        System.out.println(md5File(new File("/Users/chaoma/Downloads/等保2.0信息安全培训视频.rar")));
        stopWatch.stop();
        System.out.println(stopWatch.getTime(TimeUnit.SECONDS));
    }

    //获得字符串的md5值
    public static String md5(String str) {
        //更新摘要数据
        md.update(str.getBytes());
        //生成摘要数组
        byte[] digest = md.digest();
        //清空摘要数据，以便下次使用
        md.reset();
        return formatByteArray2Str(digest);
    }

    //获得文件的md5值
    public static String md5File(File file) throws IOException {
        //创建文件输入流
        FileInputStream fis = new FileInputStream(file);
        //将文件中的数据写入md对象
        byte[] buffer = new byte[1024];
        int len;
        while ((len = fis.read(buffer)) != -1) {
            md.update(buffer, 0, len);
        }
        fis.close();
        //生成摘要数组
        byte[] digest = md.digest();
        //清空摘要数据，以便下次使用
        md.reset();
        return formatByteArray2Str(digest);
    }

    //获得文件输入流的md5值
    public static String md5Stream(InputStream is) throws IOException {
        //将文件中的数据写入md对象
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            md.update(buffer, 0, len);
        }
        is.close();
        //生成摘要数组
        byte[] digest = md.digest();
        //清空摘要数据，以便下次使用
        md.reset();
        return formatByteArray2Str(digest);
    }

    //将摘要字节数组转换为md5值
    public static String formatByteArray2Str(byte[] digest) {
        //创建sb用于保存md5值
        StringBuilder sb = new StringBuilder();
        int temp;
        for (byte b : digest) {
            //将数据转化为0到255之间的数据
            temp = b & 0xff;
            if (temp < 16) {
                sb.append(0);
            }
            //Integer.toHexString(temp)将10进制数字转换为16进制
            sb.append(Integer.toHexString(temp));
        }
        return sb.toString();
    }
}
