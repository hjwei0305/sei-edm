package com.changhong.sei.edm.file.service.local;

import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.common.util.DocumentTypeUtil;
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
import com.changhong.sei.util.IdGenerator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
 * @version 1.0.00  2020-02-03 14:07
 */
public class LocalFileService implements FileService {
    public static final String DOT = ".";

    private String storePath;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private ModelMapper modelMapper;

    /**
     * 获取本地存储目录
     */
    public StringBuffer getFileDir() {
        if (StringUtils.isBlank(storePath)) {
            storePath = ContextUtil.getProperty("sei.edm.store-path");
            if (StringUtils.isBlank(storePath)) {
                storePath = System.getProperty("java.io.tmpdir");
            }
        }
        StringBuffer dir = new StringBuffer(32);
        dir.append(storePath);
        if (!StringUtils.endsWithAny(storePath, FileUtils.SLASH_ONE, FileUtils.SLASH_TWO)) {
            dir.append(File.pathSeparator);
        }
        return dir;
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
     * 合并文件分片
     *
     * @param fileMd5  源整文件md5
     * @param fileName 文件名
     * @return 文档信息
     */
    @Override
    public ResultData<UploadResponse> mergeFile(String fileMd5, String fileName) {
        List<FileChunk> chunks = documentService.getFileChunk(fileMd5);
        if (CollectionUtils.isNotEmpty(chunks)) {
            Set<String> chunkIds = new HashSet<>();
            Set<String> docIds = new HashSet<>();
            List<FileInputStream> inputStreamList = new ArrayList<>(chunks.size());

            // 获取文件目录
            StringBuffer fileStr = this.getFileDir();
            for (FileChunk chunk : chunks) {
                chunkIds.add(chunk.getId());
                docIds.add(chunk.getDocId());
                try {
                    File file = FileUtils.getFile(fileStr + chunk.getDocId());
                    if (file.exists()) {
                        inputStreamList.add(new FileInputStream(file));
                    }
                } catch (IOException e) {
                    LogUtil.error("[" + chunk.getDocId() + "]分片文件读取异常.", e);
                }
            }

            // 检查分片数量是否一致
            if (chunks.size() != inputStreamList.size()) {
                return ResultData.fail("分片错误");
            }

            // uuid生成新的文件名
            final String docId = IdGenerator.uuid2() + DOT + FileUtils.getExtension(fileName);

            // 异步上传持久化
            CompletableFuture.runAsync(() -> {
                //将集合中的枚举 赋值给 en
                Enumeration<FileInputStream> en = Collections.enumeration(inputStreamList);
                //en中的 多个流合并成一个
                InputStream sis = new SequenceInputStream(en);

                FileOutputStream fos = null;
                byte[] buf = new byte[1024];
                int len = 0;
                try {
                    File originFile = new File(fileStr + docId);
                    fos = new FileOutputStream(originFile);
                    while ((len = sis.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }

                    uploadDocument(fileMd5, fileName, "", "", originFile);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                        sis.close();
                    } catch (IOException ignored) {
                    }
                }

                // 删除分片文件
                removeByDocIds(docIds, true);
                // 删除分片信息
                documentService.deleteFileChunk(chunkIds);

                LogUtil.debug("异步处理完成");
            });

            UploadResponse response = new UploadResponse();
            response.setDocId(docId);
            response.setFileName(fileName);
            response.setDocumentType(DocumentTypeUtil.getDocumentType(fileName));

            return ResultData.success(response);
        } else {
            return ResultData.fail("文件分片不存在.");
        }
    }

    /**
     * 获取一个文档(不含文件内容数据)
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
     * @param docId    文档Id
     * @param hasChunk
     * @param out
     */
    @Override
    public void getDocumentOutputStream(String docId, boolean hasChunk, OutputStream out) {
        if (StringUtils.isNotBlank(docId)) {
            // 获取文件目录
            StringBuffer fileStr = this.getFileDir();
            if (hasChunk) {
                List<FileChunk> chunks = documentService.getFileChunkByOriginDocId(docId);
                if (CollectionUtils.isNotEmpty(chunks)) {
                    for (FileChunk chunk : chunks) {
                        byte[] data;
                        try {
                            File file = FileUtils.getFile(fileStr + chunk.getDocId());
                            if (file.exists()) {
                                data = FileUtils.readFileToByteArray(file);
                                out.write(data, 0, data.length);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    LogUtil.error("{} 文件的分块不存在.", docId);
                }
            } else {
                try {
                    File file = FileUtils.getFile(fileStr + docId);
                    if (file.exists()) {
                        FileInputStream input = new FileInputStream(file);
                        byte[] b = new byte[1024];
                        int len;
                        while ((len = input.read(b)) != -1) {
                            out.write(b, 0, len);
                        }
                    }
                } catch (IOException e) {
                    LogUtil.error("[" + docId + "]文件读取异常.", e);
                }
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

        Document document = documentService.findByProperty(Document.FIELD_DOC_ID, docId);
        if (Objects.nonNull(document)) {
            modelMapper.map(document, response);

            // 获取文件目录
            StringBuffer fileStr = this.getFileDir();
            if (document.getHasChunk()) {
                List<FileChunk> chunks = documentService.getFileChunkByOriginDocId(docId);
                if (CollectionUtils.isNotEmpty(chunks)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] data = null;
                    for (FileChunk chunk : chunks) {
                        try {
                            File file = FileUtils.getFile(fileStr + chunk.getDocId());
                            if (file.exists()) {
                                data = ArrayUtils.addAll(data, FileUtils.readFileToByteArray(file));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    response.setData(out.toByteArray());
                } else {
                    LogUtil.error("{} 文件的分块不存在.", docId);
                }
            } else {
                try {
                    File file = FileUtils.getFile(fileStr + document.getDocId());
                    if (file.exists()) {
                        response.setData(FileUtils.readFileToByteArray(file));
                    }
                } catch (IOException e) {
                    LogUtil.error("[" + docId + "]文件读取异常.", e);
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
    @Transactional
    public ResultData<String> removeByDocIds(Set<String> docIds, boolean isChunk) {
        if (CollectionUtils.isNotEmpty(docIds)) {
            // 删除文档信息
            if (isChunk) {
                documentService.deleteChunkByDocIdIn(docIds);
            } else {
                documentService.deleteByDocIds(docIds);
            }

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
    @Transactional
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
        document.setDocumentType(DocumentTypeUtil.getDocumentType(document.getFileName()));

        documentService.save(document);

        UploadResponse response = new UploadResponse();
        response.setDocId(document.getDocId());
        response.setFileName(document.getFileName());
        response.setDocumentType(document.getDocumentType());

        return ResultData.success(response);
    }
}
