package com.changhong.sei.edm.preview.service.impl;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.preview.service.PreviewService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Service
public class TextPreviewServiceImpl implements PreviewService {

    /**
     * 将文档转为预览文档
     *
     * @param document 需要转换的文件
     * @return 返回预览文档
     */
    @Override
    public ResultData<DocumentResponse> preview(DocumentDto document) {
        DocumentResponse response = new DocumentResponse();
        // 注意: 仅用于URLConnection.guessContentTypeFromName获取MimeType,无其他意义
        response.setFileName(document.getFileName());
        byte[] data = document.getData();
        try {
            String enc = getFileEncodeUTFGBK(data);
            if (StringUtils.equalsIgnoreCase("GBK", enc)) {
                data = convertFileEncoding(data, "GBK");
                response.setData(data);
                response.setSize((long) data.length);
            }
        } catch (Exception e) {
            LogUtil.error("文件编码转换异常", e);

            response.setData(document.getData());
            response.setSize(document.getSize());
        }

        return ResultData.success(response);
    }

    /**
     * 判断文件编码格式
     */
    private String getFileEncodeUTFGBK(byte[] data) throws IOException {
        String enc = Charset.forName("GBK").name();
        InputStream in = null;
        try {
            in = new ByteArrayInputStream(data);
            byte[] b = new byte[3];
            in.read(b);

            if (b[0] == -17 && b[1] == -69 && b[2] == -65) {
                enc = StandardCharsets.UTF_8.name();
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                    in = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // System.out.println("文件编码格式为:" + enc);
        return enc;
    }

    /**
     * 文件编码转为utf-8
     *
     * @param srcData    原文件数据
     * @param srcCharset 原文件编码
     */
    public static byte[] convertFileEncoding(byte[] srcData, String srcCharset) throws IOException {
        if (srcData == null || srcData.length == 0) {
            throw new IllegalArgumentException("srcFile is empty.");
        }

        if (srcCharset == null || srcCharset.length() == 0) {
            throw new IllegalArgumentException("srcCharset is empty.");
        }

        InputStream fis = null;
        InputStreamReader isr;
        BufferedReader br;

        ByteArrayOutputStream baos = null;
        OutputStreamWriter osw;
        try {
            fis = new ByteArrayInputStream(srcData);
            isr = new InputStreamReader(fis, srcCharset);

            // BufferedReader中defaultCharBufferSize = 8192.
            // 即：8192 × 2 byte = 16k
            // 若是utf-8,中文占3个字节，16K / 3  = 5461，即只要每行中文字符数 < 5461,读取的行数就是准确的，
            // 否则，可能会截断一行，多写入'\n',但这种情况一般不存在。
            // 如果源文件中最后一行没有换行符，转码后的文件最后会多写入一个换行符
            br = new BufferedReader(isr);

            baos = new ByteArrayOutputStream();
            osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);

            String str = null;

            // 创建StringBuffer字符串缓存区
            StringBuilder sb = new StringBuilder();

            // 通过readLine()方法遍历读取文件
            while ((str = br.readLine()) != null) {
                // 使用readLine()方法无法进行换行,需要手动在原本输出的字符串后面加"\n"或"\r"
                sb.append(str).append('\n');
            }
            osw.write(sb.toString());
            osw.flush();

            return baos.toByteArray();
        } finally {
            // 与同一个文件关联的所有输出流(输入流)，只需关闭一个即可
            if (null != fis) {
                try {
                    fis.close();
                    fis = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != baos) {
                try {
                    baos.close();
                    baos = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
