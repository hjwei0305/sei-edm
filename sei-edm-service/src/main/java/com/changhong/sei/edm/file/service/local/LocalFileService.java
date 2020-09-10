package com.changhong.sei.edm.file.service.local;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.common.constant.Constants;
import com.changhong.sei.edm.common.util.ImageUtils;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.dto.DocumentType;
import com.changhong.sei.edm.dto.UploadResponse;
import com.changhong.sei.edm.file.service.FileService;
import com.changhong.sei.edm.manager.entity.Document;
import com.changhong.sei.edm.manager.service.DocumentService;
import com.changhong.sei.util.FileUtils;
import com.changhong.sei.util.IdGenerator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-03 14:07
 */
public class LocalFileService implements FileService {
    public static final String DOT = ".";

    @Value("${sei.edm.store-path}")
    private String storePath;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private ModelMapper modelMapper;

    /**
     * 获取本地存储目录
     */
    public StringBuffer getFileDir() {
        StringBuffer dir = new StringBuffer(32);
        dir.append(storePath);
        if (!StringUtils.endsWithAny(storePath, FileUtils.SLASH_ONE, FileUtils.SLASH_TWO)) {
            dir.append(File.pathSeparator);
        }
        return dir;
    }

    /**
     * 通过文件名获取文档类型
     *
     * @param fileName 文件名
     * @return 文档类型
     */
    public DocumentType getDocumentType(String fileName) {
        String extension = FileUtils.getExtension(fileName);
        if (StringUtils.isBlank(extension)) {
            return DocumentType.Other;
        }
        extension = extension.toLowerCase();
        for (Map.Entry<DocumentType, String> entry : Constants.DOC_TYPE_MAP.entrySet()) {
            if (StringUtils.contains(entry.getValue(), extension)) {
                return entry.getKey();
            }
        }
        return DocumentType.Other;
    }

    /**
     * 上传一个文档(如果是图像生成缩略图)
     *
     * @param dto 上传dto
     * @return 文档信息
     */
    @Override
    public ResultData<UploadResponse> uploadDocument(DocumentDto dto) {
        if (Objects.isNull(dto)) {
            return ResultData.fail("文件对象为空.");
        }
        if (Objects.isNull(dto.getData())) {
            return ResultData.fail("文件流为空.");
        }

        // 获取文件目录
        StringBuffer fileStr = this.getFileDir();
        // uuid生成新的文件名
        fileStr.append(IdGenerator.uuid2()).append(DOT).append(FileUtils.getExtension(dto.getFileName()));

        File file = FileUtils.getFile(fileStr.toString());
        try {
            FileUtils.writeByteArrayToFile(file, dto.getData());
        } catch (IOException e) {
            LogUtil.error("文件上传读取异常.", e);
            return ResultData.fail("文件上传读取异常.");
        }

        return uploadDocument(dto.getFileMd5(), dto.getFileName(), dto.getSystem(), dto.getUploadUser(), file);
    }

    /**
     * 获取一个文档(包含信息和数据)
     *
     * @param docId 文档Id
     * @return 文档
     */
    @Override
    public DocumentResponse getDocumentInfo(String docId) {
        DocumentResponse response = new DocumentResponse();

        Document document = documentService.findByProperty(Document.FIELD_DOC_ID, docId);
        if (Objects.nonNull(document)) {
            modelMapper.map(document, response);
        } else {
            LogUtil.error("docId: {} 对应的文件不存在.", docId);
        }

        return response;
    }

    /**
     * 批量获取文档信息(不含文件内容数据)
     *
     * @param docIds 文档
     * @return 文档清单
     */
    @Override
    public List<DocumentResponse> getDocumentInfo(Set<String> docIds) {
        List<DocumentResponse> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(docIds)) {
            return result;
        }

        Document document;
        DocumentResponse response;
        for (String docId : docIds) {
            document = documentService.findByProperty(Document.FIELD_DOC_ID, docId);
            if (Objects.nonNull(document)) {
                response = new DocumentResponse();
                modelMapper.map(document, response);
                result.add(response);
            }
        }

        return result;
    }

    /**
     * 获取一个文档(包含信息和数据)
     *
     * @param docId 文档Id
     * @return 文档
     */
    @Override
    public DocumentResponse getDocument(String docId) {
        DocumentResponse response = new DocumentResponse();

        Document document = documentService.findByProperty(Document.FIELD_DOC_ID, docId);
        if (Objects.nonNull(document)) {
            modelMapper.map(document, response);

            // 获取文件目录
            StringBuffer fileStr = this.getFileDir();
            try {
                File file = FileUtils.getFile(fileStr + document.getDocId());
                if (file.exists()) {
                    response.setData(FileUtils.readFileToByteArray(file));
                }
            } catch (IOException e) {
                LogUtil.error("[" + docId + "]文件读取异常.", e);
            }
        }

        return response;
    }

    /**
     * 获取缩略图
     *
     * @param docId  文档Id
     * @param width  宽
     * @param height 高
     * @return 返回缩略图
     */
    @Override
    public DocumentResponse getThumbnail(String docId, int width, int height) {
        Document document = documentService.findByProperty(Document.FIELD_DOC_ID, docId);
        if (Objects.nonNull(document)) {
            //如果是图像文档，生成缩略图
            if (DocumentType.Image.equals(document.getDocumentType())) {
                DocumentResponse response = new DocumentResponse();
                modelMapper.map(document, response);

                //复制数据流
                FileInputStream imageStream = null;
                // 获取文件目录
                StringBuffer fileStr = this.getFileDir();
                try {
                    File file = FileUtils.getFile(fileStr + document.getDocId());
                    if (file.exists()) {
                        imageStream = FileUtils.openInputStream(file);

                        String ext = FileUtils.getExtension(document.getFileName());
                        byte[] thumbData = ImageUtils.scale2(imageStream, ext, height, width, true);
                        response.setData(thumbData);
                        return response;
                    }
                } catch (IOException e) {
                    LogUtil.error("生成缩略图异常.", e);
                } finally {
                    if (Objects.nonNull(imageStream)) {
                        try {
                            imageStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 删除文档
     *
     * @param docIds 文档
     * @return 删除结果
     */
    @Override
    public ResultData<String> removeByDocIds(Set<String> docIds) {
        if (CollectionUtils.isNotEmpty(docIds)) {
            // 删除文档信息
            documentService.deleteByDocIds(docIds);

            for (String docId : docIds) {
                // 获取文件目录
                StringBuffer fileStr = this.getFileDir();
                try {
                    File file = FileUtils.getFile(fileStr + docId);
                    if (file.exists()) {
                        file.delete();
                    }
                } catch (Exception e) {
                    LogUtil.error("[" + docId + "]文件删除异常.", e);
                }
            }
        }
        return ResultData.success("删除成功.");
    }

    /**
     * 清理所有文档(删除无业务信息的文档)
     */
    @Override
    public ResultData<String> removeInvalidDocuments() {
        ResultData<Set<String>> resultData = documentService.getInvalidDocIds();
        if (resultData.successful()) {
            Set<String> docIdSet = resultData.getData();
            if (CollectionUtils.isNotEmpty(docIdSet)) {
                // 删除文档
                removeByDocIds(docIdSet);
            }
            return ResultData.success("成功清理: " + docIdSet.size() + "个");
        }
        return ResultData.fail(resultData.getMessage());
    }

    /**
     * 上传一个文档
     *
     * @param file 文档
     * @return 文档信息
     */
    private ResultData<UploadResponse> uploadDocument(String md5, String originName, String sys, String uploadUser, File file) {
        if (Objects.isNull(file)) {
            return ResultData.fail("文件不存在.");
        }

        Document document = new Document(originName);
        document.setFileMd5(md5);
        document.setDocId(FileUtils.getFileName(file.getName()));
        document.setSize(file.length());
        document.setSystem(sys);
        document.setUploadUser(uploadUser);
        document.setUploadedTime(LocalDateTime.now());
        document.setDocumentType(getDocumentType(document.getFileName()));

        documentService.save(document);

        UploadResponse response = new UploadResponse();
        response.setDocId(document.getDocId());
        response.setFileName(document.getFileName());
        response.setDocumentType(document.getDocumentType());

        return ResultData.success(response);
    }
}
