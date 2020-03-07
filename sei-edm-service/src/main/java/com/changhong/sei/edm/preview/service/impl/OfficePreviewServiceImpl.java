package com.changhong.sei.edm.preview.service.impl;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.common.util.PdfUtils;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.preview.service.PreviewService;
import com.changhong.sei.util.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jodconverter.DocumentConverter;
import org.jodconverter.document.DefaultDocumentFormatRegistry;
import org.jodconverter.office.OfficeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 实现功能：Office预览服务
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-08 10:38
 */
@Service
public class OfficePreviewServiceImpl implements PreviewService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfPreviewServiceImpl.class);

    private List<String> FILE_TYPE2_HTMLS = Arrays.asList("xls", "xlsx");

    @Autowired
    private DocumentConverter documentConverter;

    /**
     * 将文档转为预览文档
     *
     * @param document 需要转换的文件
     * @return 返回预览文档
     */
    @Override
    public ResultData<DocumentResponse> preview(DocumentDto document) {
        if (Objects.isNull(document)) {
            return ResultData.fail("document不能为空.");
        }
        InputStream inputStream = new ByteArrayInputStream(document.getData());
        ResultData<DocumentResponse> result = convertInputStream(inputStream, document.getFileName(), document.getMarkText());
        try {
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @param sourceFile    需要转换的文件
     * @param targetFileDir 目标文件目录(以 / 结尾)
     */
    public ResultData<String> convertFile(File sourceFile, String targetFileDir) {
        try {
            // 无扩展名文件名
            String fileName = FileUtils.getWithoutExtension(sourceFile.getName());
            // 文件名扩展名
            String fileExt = FileUtils.getExtension(sourceFile.getName()).toLowerCase();
            // 目标文件名扩展名
            String targetFileExt = this.getTargetFileExt(fileExt);
            // 目标文件
            File targetFile = FileUtils.getFile(targetFileDir + fileName + FileUtils.DOT + targetFileExt);
            documentConverter
                    .convert(sourceFile).as(DefaultDocumentFormatRegistry.getFormatByExtension(fileExt))
                    .to(targetFile).as(DefaultDocumentFormatRegistry.getFormatByExtension(targetFileExt))
                    .execute();

            return ResultData.success(targetFile.getName());
        } catch (OfficeException e) {
            LogUtil.error("convertFile2pdf error : " + e.getMessage(), e);
            return ResultData.fail("convertFile2pdf error : " + e.getMessage());
        }
    }

    /**
     * @param in            文件输入流, 需要调用方自行关闭
     * @param fileName      文件名
     * @param targetFileDir 目标文件目录
     */
    public ResultData<File> convertInputStream2File(InputStream in, String fileName, File targetFileDir) {
        try {
            ResultData<DocumentResponse> resultData = convertInputStream(in, fileName, StringUtils.EMPTY);
            if (resultData.successful()) {
                String fileExt = FileUtils.getExtension(fileName).toLowerCase();
                String targetFileExt = this.getTargetFileExt(fileExt);
                File targetFile = FileUtils.getFile(targetFileDir, fileName + FileUtils.DOT + targetFileExt);

                FileUtils.writeByteArrayToFile(targetFile, resultData.getData().getData());

                return ResultData.success(targetFile);
            }
        } catch (IOException e) {
            LogUtil.error("convertInputStream2pdf error : " + e.getMessage(), e);
        }
        return ResultData.fail("convertInputStream2pdf error");
    }

    /**
     * @param source   需要转换的文件数据
     * @param fileName 文件名
     */
    public ResultData<DocumentResponse> convertByteArray(byte[] source, String fileName) {
        InputStream inputStream = new ByteArrayInputStream(source);
        ResultData<DocumentResponse> result = convertInputStream(inputStream, fileName, StringUtils.EMPTY);
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @param in       文件输入流
     * @param fileName 文件名
     */
    public ResultData<DocumentResponse> convertInputStream(InputStream in, String fileName, String markText) {
        if (Objects.isNull(fileName) || fileName.length() == 0) {
            return ResultData.fail("文件名不能为空.");
        }

        String fileExt = FileUtils.getExtension(fileName).toLowerCase();
        String targetFileExt = this.getTargetFileExt(fileExt);

        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            documentConverter
                    // 源
                    .convert(in).as(DefaultDocumentFormatRegistry.getFormatByExtension(fileExt))
                    // 目标
                    .to(out, false).as(DefaultDocumentFormatRegistry.getFormatByExtension(targetFileExt))
                    // 执行
                    .execute();

            DocumentResponse response = new DocumentResponse();
            // 注意: 仅用于URLConnection.guessContentTypeFromName获取MimeType,无其他意义
            response.setFileName(FileUtils.getWithoutExtension(fileName) + FileUtils.DOT + targetFileExt);
            response.setData(out.toByteArray());
            response.setSize((long) out.size());

            if (StringUtils.isNotBlank(markText) && StringUtils.equalsIgnoreCase(FileUtils.PDF, targetFileExt)) {
                try {
                    // 水印
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    PdfUtils.watermarkPDF(response.getData(), markText, outputStream);

                    response.setData(outputStream.toByteArray());
                    response.setSize((long) outputStream.size());

                    outputStream.close();
                } catch (Exception e) {
                    LOGGER.error("添加水印错误", e);
                }
            }

            return ResultData.success(response);
        } catch (Exception e) {
            LogUtil.error("convertByInputStream error : " + e.getMessage(), e);
            return ResultData.fail("convertByInputStream error : " + e.getMessage());
        } finally {
            if (Objects.nonNull(out)) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 获取想要转换的格式类型
     */
    private String getTargetFileExt(String originFileExt) {
        if (FILE_TYPE2_HTMLS.contains(originFileExt)) {
            return FileUtils.HTML;
        }
        return FileUtils.PDF;
    }
}
