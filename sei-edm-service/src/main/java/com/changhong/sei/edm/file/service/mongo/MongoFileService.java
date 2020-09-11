package com.changhong.sei.edm.file.service.mongo;

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
import com.changhong.sei.edm.manager.entity.FileChunk;
import com.changhong.sei.edm.manager.service.DocumentService;
import com.changhong.sei.util.FileUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-03 14:08
 */
public class MongoFileService implements FileService {

    @Autowired
    private MongoDbFactory mongoDbFactory;
    //    @Autowired
//    private GridFsOperations edmGridFsTemplate;
    @Autowired
    private SeiGridFsOperations seiGridFsTemplate;

    @Autowired
    private DocumentService documentService;
    @Autowired
    private ModelMapper modelMapper;

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
    @Transactional
    public ResultData<UploadResponse> uploadDocument(DocumentDto dto) {
        if (Objects.isNull(dto)) {
            return ResultData.fail("文件对象为空.");
        }

        final byte[] data = dto.getData();
        if (Objects.isNull(data)) {
            return ResultData.fail("文件流为空.");
        }

        ObjectId objectId = new ObjectId();
        String fileName = dto.getFileName();

        // 异步上传持久化
        uploadDocument(objectId, new ByteArrayInputStream(data), fileName, dto.getFileMd5(), data.length);

        UploadResponse response = new UploadResponse();
        response.setDocId(objectId.toString());
        response.setFileName(fileName);
        response.setDocumentType(getDocumentType(fileName));

        return ResultData.success(response);
    }

    /**
     * 合并文件分片
     *
     * @param fileMd5  源整文件md5
     * @param fileName 文件名
     * @return 文档信息
     */
    @Override
    @Transactional
    public ResultData<UploadResponse> mergeFile(String fileMd5, String fileName) {
        List<FileChunk> chunks = documentService.getFileChunk(fileMd5);
        if (CollectionUtils.isNotEmpty(chunks)) {
            Set<String> chunkIds = new HashSet<>();
            Set<String> docIds = new HashSet<>();
            ByteArrayOutputStream out;
            List<ByteArrayInputStream> inputStreamList = new ArrayList<>(chunks.size());
            for (FileChunk chunk : chunks) {
                chunkIds.add(chunk.getId());
                docIds.add(chunk.getDocId());

                out = getByteArray(chunk.getDocId());
                if (Objects.nonNull(out)) {
                    inputStreamList.add(new ByteArrayInputStream(out.toByteArray()));
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    return ResultData.fail("分片错误");
                }
            }

            // 检查分片数量是否一致
            if (chunks.size() != inputStreamList.size()) {
                return ResultData.fail("分片错误");
            }

            final long size = chunks.get(0).getTotalSize();
            ObjectId objectId = new ObjectId();

            // 异步上传持久化
            CompletableFuture.runAsync(() -> {
                //将集合中的枚举 赋值给 en
                Enumeration<ByteArrayInputStream> en = Collections.enumeration(inputStreamList);
                //en中的 多个流合并成一个
                InputStream sis = new SequenceInputStream(en);

                uploadDocument(objectId, sis, fileName, fileMd5, size);

                // 删除分片文件
                removeByDocIds(docIds);
                // 删除分片信息
                documentService.deleteFileChunk(chunkIds);

                LogUtil.debug("异步处理完成");
            });

            UploadResponse response = new UploadResponse();
            response.setDocId(objectId.toString());
            response.setFileName(fileName);
            response.setDocumentType(getDocumentType(fileName));

            return ResultData.success(response);
        } else {
            return ResultData.fail("文件分片不存在.");
        }
    }

    /**
     * 获取一个文档(不包含信息和数据)
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

            ByteArrayOutputStream baos = getByteArray(docId);
            if (baos != null) {
                response.setData(baos.toByteArray());
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

                //获取原图
                GridFSFile fsdbFile = seiGridFsTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(docId)));
                if (Objects.isNull(fsdbFile)) {
                    LogUtil.error("[{}]缩略图不存在.", docId);
                    return null;
                }
                GridFSBucket bucket = GridFSBuckets.create(mongoDbFactory.getDb());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bucket.downloadToStream(fsdbFile.getId(), baos);

                InputStream imageStream = null;
                try {
                    imageStream = new ByteArrayInputStream(baos.toByteArray());
                    String ext = FileUtils.getExtension(document.getFileName());
                    byte[] thumbData = ImageUtils.scale2(imageStream, ext, height, width, true);

                    response.setData(thumbData);
                    return response;
                } catch (Exception e) {
                    return null;
                } finally {
                    if (Objects.nonNull(imageStream)) {
                        try {
                            imageStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        baos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
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
    @Transactional
    public ResultData<String> removeByDocIds(Set<String> docIds) {
        if (CollectionUtils.isNotEmpty(docIds)) {
            // 删除文档信息
            documentService.deleteByDocIds(docIds);

            for (String docId : docIds) {
                try {
                    //删除文档数据
                    Query query = new Query().addCriteria(Criteria.where("_id").is(docId));
                    seiGridFsTemplate.delete(query);
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
    @Transactional
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
     * 获取文档
     *
     * @param docId 文档id
     * @return 返回输出流
     */
    private ByteArrayOutputStream getByteArray(String docId) {
        //获取原图
        GridFSFile fsdbFile = seiGridFsTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(docId)));
        if (Objects.isNull(fsdbFile)) {
            LogUtil.error("[{}]文件不存在.", docId);
            return null;
        }
        GridFSBucket bucket = GridFSBuckets.create(mongoDbFactory.getDb());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bucket.downloadToStream(fsdbFile.getId(), baos);
        return baos;
    }

    /**
     * 上传一个文档
     */
    private void uploadDocument(ObjectId objectId, InputStream inputStream, String fileName, String fileMd5, long size) {
        try {
            DocumentType documentType = getDocumentType(fileName);

            //重置数据流
            //保存数据文件
            DBObject metaData = new BasicDBObject();
            metaData.put("description", fileName);
//            ObjectId objectId = edmGridFsTemplate.store(inputStream, fileName, documentType.toString(), metaData);
            seiGridFsTemplate.store(objectId, inputStream, fileName, documentType.toString(), metaData);

            Document document = new Document(fileName);
            document.setDocId(objectId.toString());
            document.setFileMd5(fileMd5);
            document.setSize(size);
            document.setUploadedTime(LocalDateTime.now());
            document.setDocumentType(documentType);

            documentService.save(document);
        } catch (Exception e) {
            LogUtil.error("[" + objectId + "]文件上传读取异常.", e);
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
