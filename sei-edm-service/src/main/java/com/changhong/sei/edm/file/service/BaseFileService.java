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
        response.setFileMd5(document.getFileMd5());

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
            response.setFileMd5(fileMd5);

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
            // 更新docid
            response.setDocId(document.getId());
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
                // 更新docid
                response.setDocId(document.getId());
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
            // 更新docid
            response.setDocId(document.getId());

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
        DocumentResponse response = null;
        Document document = documentService.getByDocId(docId);
        if (Objects.nonNull(document)) {
            response = new DocumentResponse();
            modelMapper.map(document, response);
            // 更新docid
            response.setDocId(document.getId());

            //如果是图像文档，生成缩略图
            if (DocumentType.Image.equals(document.getDocumentType())) {
                //获取原图
                try (ByteArrayOutputStream baos = getByteArray(document.getDocId()); InputStream imageStream = new ByteArrayInputStream(baos.toByteArray())) {
                    String ext = FileUtils.getExtension(document.getFileName());
                    byte[] thumbData = ImageUtils.scale2(imageStream, ext, height, width, true);

                    response.setData(thumbData);
                } catch (IOException e) {
                    response.setData(FileUtils.decodeBase64("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAHdSURBVGhD7do9Sx1BFMbxa0BBiASRpEgXFQJpbLQIJEVACIlYCRb2fgNBBSsFia0kgaQMwVYs7JQU0c7Gly9gYidCCt+JL/9TDByWDezsntlscR74tWfm2bsLF2ZaBfMQU9jCMe4quMYORlFr3qHq5v9lBZ1InnH8Rd4mrGyjB8nShzNkF/4NeTXKOEF2njjAYyTJN+jFpMBbVMlTnEPPDZKUkff2FGGRKwzAIuvQBbR9mJYZgl7gO6zyBXp21i7MyryHHj4Hq3yGnp1nDyZlRqAHz8AqRYoIk2+mCUVE5TJNKSLkNXuEUmlSEbGJDkSnaUXEAqKTssgS9OyiLtGPqKQsIps5gp5f1DSikrKIpB3P0JvjOV5iEr+g9/ERUUldpGjk37feh3xfUWlKkVfQ+/AigRepGC8S4kWM40VCvIhxvEiIFzGOFwnxIsbxIiFexDheJMSLGMeLhHgR42SLLCMqlZ+EUSag9zGLqMgBvr4oIDcfnqDOPMBP6CKlrn1sQA/5ATknryNyPP4Jev0/KHXd4zVukR0m90fkjONDIl9xCL2uqPSdLiI78H+Qozc5iiidNswj+8vUaQ1dMMkgVnGBvMWs3WAbY5CHaR752F7gDYYTkTsv3YhIq3UP+xNmATtl2PgAAAAASUVORK5CYII="));
                }
            } else if (DocumentType.Compressed.equals(document.getDocumentType())) {
                response.setData(FileUtils.decodeBase64("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAHdSURBVGhD7do9Sx1BFMbxa0BBiASRpEgXFQJpbLQIJEVACIlYCRb2fgNBBSsFia0kgaQMwVYs7JQU0c7Gly9gYidCCt+JL/9TDByWDezsntlscR74tWfm2bsLF2ZaBfMQU9jCMe4quMYORlFr3qHq5v9lBZ1InnH8Rd4mrGyjB8nShzNkF/4NeTXKOEF2njjAYyTJN+jFpMBbVMlTnEPPDZKUkff2FGGRKwzAIuvQBbR9mJYZgl7gO6zyBXp21i7MyryHHj4Hq3yGnp1nDyZlRqAHz8AqRYoIk2+mCUVE5TJNKSLkNXuEUmlSEbGJDkSnaUXEAqKTssgS9OyiLtGPqKQsIps5gp5f1DSikrKIpB3P0JvjOV5iEr+g9/ERUUldpGjk37feh3xfUWlKkVfQ+/AigRepGC8S4kWM40VCvIhxvEiIFzGOFwnxIsbxIiFexDheJMSLGMeLhHgR42SLLCMqlZ+EUSag9zGLqMgBvr4oIDcfnqDOPMBP6CKlrn1sQA/5ATknryNyPP4Jev0/KHXd4zVukR0m90fkjONDIl9xCL2uqPSdLiI78H+Qozc5iiidNswj+8vUaQ1dMMkgVnGBvMWs3WAbY5CHaR752F7gDYYTkTsv3YhIq3UP+xNmATtl2PgAAAAASUVORK5CYII="));
            } else if (DocumentType.Word.equals(document.getDocumentType())) {
                response.setData(FileUtils.decodeBase64("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAPCSURBVGhD7dpbyE1pHMfxl3DjkEPjeCMkpIgrco4JI0mR3IymXJhJCRfOCoXignLj0ESSQySHxIWZxB1XNFGoqXEOSWTk9P2ttf7693jebdbe71r7ndq/+rTXu55nP3v933ftZz1r9TYVnK6YhGU4hJtYjFadvvgJa3EC9/E5YglaRdpgIOZjKy7gEWIHHVOXQtpjBH7GLlzBK8QO8L8qvJBOGItfsRc38C9iB1OLwgo5gr/wAbEPbmlWyDjMq9Ec/IAksQ8rkhVyDrH2vB6gM6KNRWrpQkRfiWhDkYooRKdptKFIjUIqqGsh1cxaupbFxqxrIdVkIWJjNgqpRSmFaJntswe+XX6Hz0GEfZKBs2jJ49uskP24l9MT+LHMN4VomdIRFn2ob5fwN6p7jbBPN1hUuG+z9xc+a/nf5kSE7aPhoxsn3/4YPrfg20srRL9hixZkvu0t2sGnCz7B+lyGRSvqcDFqhRzFi5zewI9looUchs9TWNs17cjSIXtV7sL66Htlif1Fw1MzT/TecDyJFnIbPn/A2nZqR5Yx2auiW1vr85t2ZFkO229KK+QjdLpY/My1QDuy6CAta2B9pmhHFp0+tt+UVohMhsVPn/21g/TGyXQzyXRYnz7akcWfcsYKGYmpOeVeoqyExc5zfVcss/F3upmkF9TnZfJTmh7wYxorpJRF4zFYbOY6k/yUZhO0r2fyU5qHuJpuJvkRfkxTaiE6JXz011ifbibRIyD10yllOY996WaSdQjHlVILke6w/Ilp6WaSZ1AfX9xmrEg3k5xGOKZYIYuwLaeLiI1ZsRB/4Jq59PhTGQDro4O1zMWMdDOJHgr48YwVUk303tiYFQtZDcsv2auiJ4zW5x/tyDIcQ9LN5PGpH8srvRA/vQ7NXpUd8P00FSu69mhJouh5k+/jlV6In159/JVeZiHMFvg+nhWyCsdz0tPP2JgVCxE/vSpt8Rq+z0aEae5LKVZIabOWzITPMIR9ziLMc4T9TF0K2QAfTZlhH10IfQYh7OPVpRBdL647zU2pOnetz51sX3OsEE0MupPMQ9ep2JjfLaQIVkg10XtjYzYKqYUVouuPVgl5aEkUG7OuhZT6ZS9Co5AKGoXUoqULeYfkOYGW4rEORbFCqrmxCukfFibga3RfrhsprUh1v66rsx4LxQ6kVlZIadESYjyW4gC0DHmP2MHlUXohsegx6SjojnE3riFc1n9PqygkFt2jDIZugbfjEvzz4lCrLaS59IMe5OlW4BTs357+d4XEokdM4V1nC6Sp6QtSRgJM/fjNPgAAAABJRU5ErkJggg=="));
            } else if (DocumentType.Excel.equals(document.getDocumentType())) {
                response.setData(FileUtils.decodeBase64("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAANlSURBVGhD7dpZqIxhHMfxYy2y78sViitZcmONkKTkBjeKLBeUJblQSi4UroRQ4oILa3GBCFEkpXChbKFkC2UJ2Zfvb+Y85/x7+r9zZpp55z00v/p0Zt7nP++Z/znv+57nfebUpZwumIhVOIDbWIJmnX6YgXU4hsf441iKZpEWGIQ52IQzeAnvTXsyaaQNhmE+tuEyPsB7g8VKvZEOGINl2IMb+AbvzZQjtUYO4g5+wvvGlRYaGYfZZZqFnsjF+2ZpCo2cgjdequfoCHcwTZVuRHRKuANpSqMRHabuQJpqjRSQaSNJV63PeBNti52A3WemjSTlPZ7kHyZGMwq7z1oj5ahqI98xBHFOw6u/h9aw0fzMqw2N7MUjxy/8iLbF3sHus+BvRFeVOGPh1c6Fje5DXsOrDY1U9ao1DXEuwdbchKb2Njtha6xMGrkLTeVtpsLWTIfNCBSagIZGDuOtQzU6vLyx4AvsPos62VcgznVo7EruWWP0m7mGeB9WUye7alK5aunE6gEbTZ81Nj73rDGLEb8+llkjsgs2LXEy/7Ah3ZF0gluZNqJjfihsutV/DdkN77Wx0MhwTHGo5lW0LXYIdp9FNyIXkZRR0AnqvS4WGsl00jgJXo7Dq/dk3shDtIOXyfBe48m0kd/Q3w+bTvVfQ+LjNkloZAE2O1SjJSdvLIgv8UU3sh82nbEh/7AhWmksZs0rNJIU1aRy1dIlNf47shov0D73rDEr4e3DyqyRebDRIaU7OI2t1QaTVriFeB9WJo2cRZyNCOOa93SFzWjonLL7sUIj+iEcdahGt7veWKA5oN1nwUY+YQBsekPbbZ1+OnG05GprrNBI1a5aaxBnB+I6/fT6wkbTlXD4xaraiBawdbzb6KME3Tl69fFcTFkEr7aqjegYPB95AK9W1GBcfwHeuRIa0eq/zq+Yap5G22JbYffZ5MmehtBIUlST2uy3kv67RvpgoEM1+qjAGwviW4ZMG8ls9lsptUYKqDVSjko38hW5mcWz+g3VEhpJurEqhS7DE9AQfcSrO0DNSI/gPopdTChVaKRq0RRCC3DLsQ+afyXNtUpR9Ua8tMVILMR2XMVHeG84SbNoxItWHQdD/2SzBedQaMWx2TaSlP6YifXQulf4t6d/rhEvWmbtlX9YydTV/QVpuS0oF4k3JwAAAABJRU5ErkJggg=="));
            } else if (DocumentType.Pdf.equals(document.getDocumentType())) {
                response.setData(FileUtils.decodeBase64("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAM2SURBVGhD3dpJyE1hHMfxlwwRmUIpylDKxoZCFDKLjWHBAguWkhQKC5mnhQxhIyGlSAkLQ6aNUKYshIwbyUxm3991T/07XXWf5zzPeW5+9ene9957nvP833PPOc95zm2qM+2wBFfwCr8L+IZrmIRSMxFFO/8v+9Ea0TMTP1CrE6GcR0dES198Qn7Fz3Dd0xvk2xO91wlRcgB2ZSpgPIqkJz7DtptRMcG3TBt8RLaSrxiIEDkFW4B1A10QLINhV3AQobIHtu28O+iGINGh0Ta+AqGyC7btWu4hSDGTYRtehlCppxAJUkwjFCJ3UaiYRilEbqMDvNJIhcg5tIJzGq0QWQvnxCxkI2zb9dK5rB+cErMQdeY5bPv1WgqnxCxEaYne6FNDfwzFfDyF7ccOOCV2IfVGo2/bD+1fTilayEjcwgss1gueGQ7bj1ILaQ4V8Au6GtSjvkY+SVqIhupa5iY2V5/rK+KTpIV0hrbCS6yBll8AnyQtRNF1hZZ7VH2cDp8kL2QV7PK94JPkhXRFdkl7Xy94JnkhSnbNv7vyl18aopDT0LLa6XUA8EnyQjTktpMXh+CT5IVMgJY7Cp1P9HwGXJO8kH3QcrOhQeAHaBg+Gi5JWoiusTVDqaOWjl7KWKiQ91DnNKs4FSuxBTpc15r8S1rIemiZbZW//kbnkSPQ6zrr/6w+z9OQxiZZId3xDprj1T6hQ+9j2LYsFaQC1UFtsdewSVZItm98rz6KtoB2eG2hWRiGY8jety7CpvRCdLidC/uV0aT0HGT7ST7qpDp2CbqBtBe6QrQprRBNdi+CTnrZZ3U3ahxCpJRChuAh7Od0ja3iQiV6IbrFkN0E0ux59rl5CJnohWSH0uN4UH2u15ohZKIXshX2/ZNoi9CJXog6vQlnsBAtECOl7Oxl5L8tZDucUvg/ESgaCdh+LIdTdGfV/lBAv3wIdoOyzmii7zJsIVPgnLOwjVxAD5QRnVR3wq7/LbxOtiOgAV++scPQPY4NkWjM9QR2vVJoP12HfIMp6NabbkV4R2fq1chvmTKdQHsEySBoOPIFtVYWmi4HrmIaQg97KtHONgCjMCYSDUgdfynU1PQHMtjeZ6qiO4QAAAAASUVORK5CYII="));
            } else {
                response.setData(FileUtils.decodeBase64("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAHdSURBVGhD7do9Sx1BFMbxa0BBiASRpEgXFQJpbLQIJEVACIlYCRb2fgNBBSsFia0kgaQMwVYs7JQU0c7Gly9gYidCCt+JL/9TDByWDezsntlscR74tWfm2bsLF2ZaBfMQU9jCMe4quMYORlFr3qHq5v9lBZ1InnH8Rd4mrGyjB8nShzNkF/4NeTXKOEF2njjAYyTJN+jFpMBbVMlTnEPPDZKUkff2FGGRKwzAIuvQBbR9mJYZgl7gO6zyBXp21i7MyryHHj4Hq3yGnp1nDyZlRqAHz8AqRYoIk2+mCUVE5TJNKSLkNXuEUmlSEbGJDkSnaUXEAqKTssgS9OyiLtGPqKQsIps5gp5f1DSikrKIpB3P0JvjOV5iEr+g9/ERUUldpGjk37feh3xfUWlKkVfQ+/AigRepGC8S4kWM40VCvIhxvEiIFzGOFwnxIsbxIiFexDheJMSLGMeLhHgR42SLLCMqlZ+EUSag9zGLqMgBvr4oIDcfnqDOPMBP6CKlrn1sQA/5ATknryNyPP4Jev0/KHXd4zVukR0m90fkjONDIl9xCL2uqPSdLiI78H+Qozc5iiidNswj+8vUaQ1dMMkgVnGBvMWs3WAbY5CHaR752F7gDYYTkTsv3YhIq3UP+xNmATtl2PgAAAAASUVORK5CYII="));
            }
        }

        return response;
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

    public ResultData<String> removeInvalidDocumentsFallback() {
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
