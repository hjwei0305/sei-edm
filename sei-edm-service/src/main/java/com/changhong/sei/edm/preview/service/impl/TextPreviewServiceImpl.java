package com.changhong.sei.edm.preview.service.impl;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.preview.service.PreviewService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.*;
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
            } else {
                response.setData(data);
                response.setSize(document.getSize());
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
        String charset = "GBK";
        byte[] first3Bytes = new byte[3];
        try (BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data))) {
            boolean checked = false;
            bis.mark(0);
            int read = bis.read(first3Bytes, 0, 3);
            if (read == -1) {
                //文件编码为 ANSI
                return charset;
            } else if (first3Bytes[0] == (byte) 0xFF
                    && first3Bytes[1] == (byte) 0xFE) {
                //文件编码为 Unicode
                charset = "UTF-16LE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xFE
                    && first3Bytes[1] == (byte) 0xFF) {
                //文件编码为 Unicode big endian
                charset = "UTF-16BE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xEF
                    && first3Bytes[1] == (byte) 0xBB
                    && first3Bytes[2] == (byte) 0xBF) {
                //文件编码为 UTF-8
                charset = "UTF-8";
                checked = true;
            }
            bis.reset();
            if (!checked) {
                while ((read = bis.read()) != -1) {
                    if (read >= 0xF0) {
                        break;
                    }
                    // 单独出现BF以下的，也算是GBK
                    if (0x80 <= read && read <= 0xBF) {
                        break;
                    }
                    if (0xC0 <= read && read <= 0xDF) {
                        read = bis.read();
                        // 双字节 (0xC0 - 0xDF)
                        if (0x80 <= read && read <= 0xBF) {
                            // (0x80
                            // - 0xBF),也可能在GB编码内
                            continue;
                        } else {
                            break;
                        }
                    }
                    // 也有可能出错，但是几率较小
                    else if (0xE0 <= read) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) {
                            read = bis.read();
                            if (0x80 <= read && read <= 0xBF) {
                                charset = "UTF-8";
                                break;
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return charset;
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

        try (
                InputStream fis = new ByteArrayInputStream(srcData);
                InputStreamReader isr = new InputStreamReader(fis, srcCharset);
                BufferedReader br = new BufferedReader(isr);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8)
        ) {
            // BufferedReader中defaultCharBufferSize = 8192.
            // 即：8192 × 2 byte = 16k
            // 若是utf-8,中文占3个字节，16K / 3  = 5461，即只要每行中文字符数 < 5461,读取的行数就是准确的，
            // 否则，可能会截断一行，多写入'\n',但这种情况一般不存在。
            // 如果源文件中最后一行没有换行符，转码后的文件最后会多写入一个换行符

            String str;
            // 创建StringBuffer字符串缓存区
            StringBuilder sb = new StringBuilder();

            // 通过readLine()方法遍历读取文件
            while ((str = br.readLine()) != null) {
                // 使用readLine()方法无法进行换行,需要手动在原本输出的字符串后面加"\n"或"\r"
                sb.append(str).append("\n\r");
            }
            osw.write(sb.toString());
            osw.flush();

            return baos.toByteArray();
        }
    }
}
