package com.changhong.sei.edm.preview.service.impl;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.preview.service.PreviewService;
import com.chonghong.sei.util.FileUtils;
import org.jodconverter.DocumentConverter;
import org.jodconverter.document.DefaultDocumentFormatRegistry;
import org.jodconverter.office.OfficeException;
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
        return convertInputStream(new ByteArrayInputStream(document.getData()), document.getFileName());
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
     * @param in            文件输入流
     * @param fileName      文件名
     * @param targetFileDir 目标文件目录
     */
    public ResultData<File> convertInputStream2File(InputStream in, String fileName, File targetFileDir) {
        try {
            ResultData<DocumentResponse> resultData = convertInputStream(in, fileName);
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
        return convertInputStream(new ByteArrayInputStream(source), fileName);
    }

    /**
     * @param in       文件输入流
     * @param fileName 文件名
     */
    public ResultData<DocumentResponse> convertInputStream(InputStream in, String fileName) {
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
            return ResultData.success(response);
        } catch (OfficeException e) {
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
