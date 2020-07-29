package com.changhong.sei.edm.file.service;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.dto.DocumentResponse;

import java.io.File;
import java.io.InputStream;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-07-29 08:56
 */
public interface FileConverterService {

    /**
     * 转为pdf文件并存储
     * 目前支持Word,Powerpoint转为pdf文件
     *
     * @param docId    文档id
     * @param markText 文档水印
     * @return 返回成功转为pdf存储的docId, 不能成功转为pdf的返回原docId
     */
    ResultData<String> convert2PdfAndSave(String docId, String markText);

    /**
     * @param sourceFile    需要转换的文件
     * @param targetFileDir 目标文件目录(以 / 结尾)
     */
    ResultData<String> convertFile(File sourceFile, String targetFileDir);

    /**
     * @param in            文件输入流, 需要调用方自行关闭
     * @param fileName      文件名
     * @param targetFileDir 目标文件目录
     */
    ResultData<File> convertInputStream2File(InputStream in, String fileName, File targetFileDir);

    /**
     * @param source   需要转换的文件数据
     * @param fileName 文件名
     */
    ResultData<DocumentResponse> convertByteArray(byte[] source, String fileName);

    /**
     * @param source   需要转换的文件数据
     * @param fileName 文件名
     */
    ResultData<DocumentResponse> convertByteArray(byte[] source, String fileName, String markText);

    /**
     * @param in       文件输入流
     * @param fileName 文件名
     */
    ResultData<DocumentResponse> convertInputStream(InputStream in, String fileName, String markText);
}
