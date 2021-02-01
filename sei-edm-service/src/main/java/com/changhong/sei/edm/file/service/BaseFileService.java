package com.changhong.sei.edm.file.service;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.limiter.support.lock.SeiLock;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.common.util.DocumentTypeUtil;
import com.changhong.sei.edm.common.util.ImageUtils;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.dto.DocumentType;
import com.changhong.sei.edm.dto.UploadResponse;
import com.changhong.sei.edm.manager.entity.Document;
import com.changhong.sei.edm.manager.entity.FileChunk;
import com.changhong.sei.edm.manager.service.DocumentService;
import com.changhong.sei.util.FileUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-03 00:32
 */
public abstract class BaseFileService implements FileService {

    @Autowired
    protected DocumentService documentService;
    @Autowired
    protected ModelMapper modelMapper;

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

        String fileName = dto.getFileName();
        Document document = new Document(fileName);
        UploadResponse response = new UploadResponse();
        Document docFile = documentService.getDocumentByMd5(dto.getFileMd5());
        if (Objects.isNull(docFile)) {
            ObjectId objectId = new ObjectId();
            // 异步上传持久化
            ResultData<Void> resultData = storeDocument(objectId.toString(), new ByteArrayInputStream(data), fileName, dto.getFileMd5(), data.length);
            if (resultData.failed()) {
                return ResultData.fail(resultData.getMessage());
            }

            document.setDocId(objectId.toString());
        } else {
            document.setDocId(docFile.getDocId());
        }
        document.setFileMd5(dto.getFileMd5());
        document.setSize((long) data.length);
        document.setUploadedTime(LocalDateTime.now());
        document.setDocumentType(DocumentTypeUtil.getDocumentType(fileName));
        documentService.save(document);

        //response.setDocId(document.getDocId());
        response.setDocId(document.getId());
        response.setFileName(document.getFileName());
        response.setDocumentType(document.getDocumentType());

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
            List<ByteArrayInputStream> inputStreamList = new ArrayList<>(chunks.size());
            for (FileChunk chunk : chunks) {
                chunkIds.add(chunk.getId());
                docIds.add(chunk.getDocId());

                try (ByteArrayOutputStream out = getByteArray(chunk.getDocId())) {
                    inputStreamList.add(new ByteArrayInputStream(out.toByteArray()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // 检查分片数量是否一致
            if (chunks.size() != inputStreamList.size()) {
                return ResultData.fail("分片错误");
            }

            final long size = chunks.get(0).getTotalSize();
            ObjectId objectId = new ObjectId();
            DocumentType documentType = DocumentTypeUtil.getDocumentType(fileName);
            // 异步上传持久化
            CompletableFuture.runAsync(() -> {
                //将集合中的枚举 赋值给 en
                Enumeration<ByteArrayInputStream> en = Collections.enumeration(inputStreamList);
                //en中的 多个流合并成一个
                InputStream sis = new SequenceInputStream(en);

                ResultData<Void> resultData = storeDocument(objectId.toString(), sis, fileName, fileMd5, size);
                inputStreamList.clear();
                if (resultData.failed()) {
                    LogUtil.debug("合并文件分片错误: " + resultData.getMessage());
                    return;
                }

                Document document = new Document(fileName);
                document.setDocId(objectId.toString());
                document.setFileMd5(fileMd5);
                document.setSize(size);
                document.setUploadedTime(LocalDateTime.now());
                document.setDocumentType(documentType);
                documentService.save(document);

                // 删除分片文件
                removeByDocIds(docIds, true);
                // 删除分片信息
                documentService.deleteFileChunk(chunkIds);

                LogUtil.debug("异步处理完成");
            });

            UploadResponse response = new UploadResponse();
            response.setDocId(objectId.toString());
            response.setFileName(fileName);
            response.setDocumentType(documentType);

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

        Document document = documentService.getByDocId(docId);
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
            document = documentService.getByDocId(docId);
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
     */
    @Override
    public void getDocumentOutputStream(String docId, boolean hasChunk, OutputStream out) {
        if (StringUtils.isNotBlank(docId)) {
            if (hasChunk) {
                List<FileChunk> chunks = documentService.getFileChunkByOriginDocId(docId);
                if (CollectionUtils.isNotEmpty(chunks)) {
                    for (FileChunk chunk : chunks) {
                        try (ByteArrayOutputStream byteArrayOut = getByteArray(chunk.getDocId())) {
                            byte[] data = byteArrayOut.toByteArray();
                            out.write(data, 0, data.length);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    LogUtil.error("{} 文件的分块不存在.", docId);
                }
            } else {
                getDocByteArray(docId, out);
            }
        }
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

        Document document = documentService.getByDocId(docId);
        if (Objects.nonNull(document)) {
            modelMapper.map(document, response);

            if (document.getHasChunk()) {
                List<FileChunk> chunks = documentService.getFileChunkByOriginDocId(docId);
                if (CollectionUtils.isNotEmpty(chunks)) {
                    byte[] data = null;
                    for (FileChunk chunk : chunks) {
                        try (ByteArrayOutputStream out = getByteArray(chunk.getDocId())) {
                            data = ArrayUtils.addAll(data, out.toByteArray());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    response.setData(data);
                } else {
                    LogUtil.error("{} 文件的分块不存在.", docId);
                }
            } else {
                try (ByteArrayOutputStream baos = getByteArray(document.getDocId())) {
                    response.setData(baos.toByteArray());
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
        Document document = documentService.getByDocId(docId);
        if (Objects.nonNull(document)) {
            //如果是图像文档，生成缩略图
            if (DocumentType.Image.equals(document.getDocumentType())) {
                DocumentResponse response = new DocumentResponse();
                modelMapper.map(document, response);

                //获取原图
                try (ByteArrayOutputStream baos = getByteArray(document.getDocId()); InputStream imageStream = new ByteArrayInputStream(baos.toByteArray())) {
                    String ext = FileUtils.getExtension(document.getFileName());
                    byte[] thumbData = ImageUtils.scale2(imageStream, ext, height, width, true);

                    response.setData(thumbData);
                    return response;
                } catch (IOException e) {
                    return null;
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
    public ResultData<String> removeByDocIds(Set<String> docIds, boolean isChunk) {
        if (CollectionUtils.isNotEmpty(docIds)) {
            // 删除文档信息
            if (isChunk) {
                documentService.deleteChunkByDocIdIn(docIds);
            } else {
                documentService.deleteByDocIds(docIds);
            }

            deleteDocuments(docIds);
        }
        return ResultData.success("删除成功.");
    }

    /**
     * 清理所有文档(删除无业务信息的文档)
     */
    @Override
    @Transactional
    @SeiLock(key = "'sei:edm:removeInvalidDocument'", fallback = "removeInvalidDocumentsFallback")
    public ResultData<String> removeInvalidDocuments() {
        long count = 0;
        // 获取未关联业务的分块
        ResultData<Set<String>> resultData = documentService.getInvalidChunkDocIds();
        if (resultData.successful()) {
            Set<String> docIdSet = resultData.getData();
            if (CollectionUtils.isNotEmpty(docIdSet)) {
                // 删除文档
                ResultData<String> removeResult = removeByDocIds(docIdSet, true);
                if (removeResult.failed()) {
                    LogUtil.error("清理过期无业务信息的文档失败: {}", removeResult.getMessage());
                }
            }
            count = docIdSet.size();
        } else {
            LogUtil.error("清理过期无业务信息的文档失败: {}", resultData.getMessage());
        }

        // 获取无效文档id(无业务信息的文档)
        resultData = documentService.getInvalidDocIds();
        if (resultData.successful()) {
            Set<String> docIdSet = resultData.getData();
            if (CollectionUtils.isNotEmpty(docIdSet)) {
                // 删除文档
                ResultData<String> removeResult = removeByDocIds(docIdSet, false);
                if (removeResult.failed()) {
                    LogUtil.error("清理过期无业务信息的文档失败: {}", removeResult.getMessage());
                }
            }
            count += docIdSet.size();
        } else {
            LogUtil.error("清理过期无业务信息的文档失败: {}", resultData.getMessage());
        }
        return ResultData.success("成功清理: " + count + "个");
    }

    public final ResultData<String> removeInvalidDocumentsFallback() {
        return ResultData.fail("临时文件清理正在清理中");
    }

    /**
     * 获取文档
     *
     * @param docId 文档id
     * @return 返回输出流
     */
    private ByteArrayOutputStream getByteArray(String docId) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.getDocByteArray(docId, baos);
        return baos;
    }

    /**
     * 获取文档
     *
     * @param docId 文档id
     */
    public abstract void getDocByteArray(String docId, OutputStream out);

    /**
     * 删除文件
     *
     * @param docIds 文档id清单
     */
    public abstract void deleteDocuments(Collection<String> docIds);

    /**
     * 上传一个文档
     */
    public abstract ResultData<Void> storeDocument(String objectId, InputStream inputStream, String fileName, String fileMd5, long size);
}
