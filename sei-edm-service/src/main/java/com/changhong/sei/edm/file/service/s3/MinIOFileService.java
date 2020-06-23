package com.changhong.sei.edm.file.service.s3;

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
import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 实现功能：MinIO是在Apache License v2.0下发布的对象存储服务器
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-04-24 09:28
 */
public class MinIOFileService implements FileService {

    @Autowired
    private DocumentService documentService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private MinioClient minioClient;
    @Value("${sei.edm.minio.bucket:sei-edm}")
    private String bucketName;

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

        String fileName = dto.getFileName();
        byte[] data = dto.getData();
        Document document;
        try (InputStream dataStream = new ByteArrayInputStream(data)) {
            DocumentType documentType = getDocumentType(fileName);

            // Check if the bucket already exists.
            boolean isExist = minioClient.bucketExists(bucketName);
            if (!isExist) {
                // Make a new bucket called asiatrip to hold a zip file of photos.
                minioClient.makeBucket(bucketName);
            }

            String objectId = IdGenerator.uuid2();
            // Upload file to the bucket with putObject
            minioClient.putObject(bucketName, objectId, dataStream, new PutObjectOptions(dataStream.available(), -1));

            document = new Document(fileName);
            document.setDocId(objectId);
            document.setSize((long) data.length);
            document.setSystem(dto.getSystem());
            document.setUploadedTime(LocalDateTime.now());
            document.setDocumentType(documentType);
        } catch (Exception e) {
            LogUtil.error("文件上传读取异常.", e);
            return ResultData.fail("文件上传读取异常.");
        }

        return uploadDocument(document, data);
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

            try (InputStream in = minioClient.getObject(bucketName, docId)) {
                response.setData(IOUtils.toByteArray(in));
            } catch (Exception e) {
                e.printStackTrace();
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

                try (InputStream imageStream = minioClient.getObject(bucketName, docId)) {
                    String ext = FileUtils.getExtension(document.getFileName());
                    byte[] thumbData = ImageUtils.scale2(imageStream, ext, height, width, true);

                    response.setData(thumbData);
                    return response;
                } catch (Exception e) {
                    LogUtil.error("获取缩略图异常.", e);
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

            try {
                //删除文档数据
                minioClient.removeObjects(bucketName, docIds);
            } catch (Exception e) {
                LogUtil.error("文件删除异常.", e);
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
     * @param document 文档
     * @return 文档信息
     */
    private ResultData<UploadResponse> uploadDocument(Document document, byte[] data) {
        if (Objects.isNull(document)) {
            return ResultData.fail("文档不能为空.");
        }

        documentService.save(document);

        UploadResponse response = new UploadResponse();
        response.setDocId(document.getDocId());
        response.setFileName(document.getFileName());
        response.setDocumentType(document.getDocumentType());

        return ResultData.success(response);
    }
}
