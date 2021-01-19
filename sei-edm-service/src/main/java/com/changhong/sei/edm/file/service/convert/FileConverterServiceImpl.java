package com.changhong.sei.edm.file.service.convert;

import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.common.util.PdfUtils;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.dto.DocumentType;
import com.changhong.sei.edm.dto.UploadResponse;
import com.changhong.sei.edm.file.service.FileConverterService;
import com.changhong.sei.edm.file.service.FileService;
import com.changhong.sei.util.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.core.office.OfficeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-07-29 08:59
 */
@Service
public class FileConverterServiceImpl implements FileConverterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileConverterServiceImpl.class);

    private final List<String> FILE_TYPE2_HTMLS = Arrays.asList("xls", "xlsx");

    @Autowired
    private DocumentConverter documentConverter;
    @Autowired
    private FileService fileService;

    /**
     * 转为pdf文件并存储
     * 目前支持Word,Powerpoint转为pdf文件
     *
     * @param docId    文档id
     * @param markText 文档水印
     * @return 返回成功转为pdf存储的docId, 不能成功转为pdf的返回原docId
     */
    @Override
    public ResultData<String> convert2PdfAndSave(String docId, String markText) {
        DocumentResponse response = fileService.getDocument(docId);
        if (Objects.isNull(response)) {
            return ResultData.fail(ContextUtil.getMessage("{} 文档不存在", docId));
        }

        if (Objects.equals(DocumentType.Word, response.getDocumentType())
                || Objects.equals(DocumentType.Powerpoint, response.getDocumentType())) {
            ResultData<DocumentResponse> resultData = convertByteArray(response.getData(), response.getFileName(), markText);
            if (resultData.successful()) {
                DocumentResponse docResponse = resultData.getData();
                ResultData<UploadResponse> uploadResult = fileService.uploadDocument(docResponse);
                if (uploadResult.successful()) {
                    UploadResponse uploadResponse = uploadResult.getData();
                    return ResultData.success(uploadResponse.getDocId());
                } else {
                    return ResultData.fail(uploadResult.getMessage());
                }
            } else {
                return ResultData.fail(resultData.getMessage());
            }
        } else {
            return ResultData.success(docId);
        }
    }

    /**
     * @param sourceFile    需要转换的文件
     * @param targetFileDir 目标文件目录(以 / 结尾)
     */
    @Override
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
    @Override
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
    @Override
    public ResultData<DocumentResponse> convertByteArray(byte[] source, String fileName) {
        return convertByteArray(source, fileName, StringUtils.EMPTY);
    }

    /**
     * @param source   需要转换的文件数据
     * @param fileName 文件名
     */
    @Override
    public ResultData<DocumentResponse> convertByteArray(byte[] source, String fileName, String markText) {
        ResultData<DocumentResponse> result;
        try (InputStream inputStream = new ByteArrayInputStream(source)) {
            result = convertInputStream(inputStream, fileName, markText);
        } catch (IOException e) {
            LOGGER.error("转换的文件数据异常", e);
            result = ResultData.fail(e.getMessage());
        }
        return result;
    }

    /**
     * @param in       文件输入流
     * @param fileName 文件名
     */
    @Override
    public ResultData<DocumentResponse> convertInputStream(InputStream in, String fileName, String markText) {
        if (Objects.isNull(fileName) || fileName.length() == 0) {
            return ResultData.fail("文件名不能为空.");
        }

        String fileExt = FileUtils.getExtension(fileName).toLowerCase();
        String targetFileExt = this.getTargetFileExt(fileExt);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
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
                // 水印
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    PdfUtils.watermarkPDF(response.getData(), markText, outputStream);

                    response.setData(outputStream.toByteArray());
                    response.setSize((long) outputStream.size());
                } catch (Exception e) {
                    LOGGER.error("添加水印错误", e);
                }
            }

            return ResultData.success(response);
        } catch (Exception e) {
            LogUtil.error("convertByInputStream error : " + e.getMessage(), e);
            return ResultData.fail("convertByInputStream error : " + e.getMessage());
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
