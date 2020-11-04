package com.changhong.sei.edm.sdk;

import com.changhong.sei.apitemplate.ApiTemplate;
import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.common.util.DocumentTypeUtil;
import com.changhong.sei.edm.common.util.MD5Utils;
import com.changhong.sei.edm.dto.*;
import com.changhong.sei.exception.ServiceException;
import com.changhong.sei.util.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.validation.constraints.NotBlank;
import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-04-20 22:42
 */
public class DocumentManager implements ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentManager.class);

    private ApplicationContext context;

    private final ApiTemplate apiTemplate;

    public DocumentManager(ApiTemplate apiTemplate) {
        this.apiTemplate = apiTemplate;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    private String getServiceUrl() {
        String host = context.getEnvironment().getProperty("sei.edm.service.url");
        if (StringUtils.isEmpty(host)) {
            throw new IllegalArgumentException("EDM服务地址未配置[sei.edm.service.url]");
        }
        return host;
    }

    /**
     * 文件分块上传
     * step1.文件分块
     * step2.分块上传
     * step3.合并分块
     *
     * @param fileName 文件名
     * @param file     文件
     * @return 返回上传结果
     */
    public ResultData<UploadResponse> uploadChunk(final String fileName, File file) throws IOException {
        // 获取文件md5
        StopWatch stopWatch = StopWatch.createStarted();
        String fileMd5 = MD5Utils.md5File(file);
        stopWatch.stop();
        LOG.debug("MD5 耗时: {}", stopWatch.getTime());

        // 检查文件分块情况
        Map<String, String> params = new HashMap<>();
        params.put("fileMd5", fileMd5);
        ResultData<FileChunkResponse> resultData = apiTemplate.getByUrl(getServiceUrl() + "/file/checkChunk",
                new ParameterizedTypeReference<ResultData<FileChunkResponse>>() {
                }, params);
        if (resultData.failed()) {
            return ResultData.fail(resultData.getMessage());
        }

        // 总大小
        long totalSize;
        // 分块大小 50mb
        int chunkSize;
        // 总块数
        int totalChunks;

        Set<Integer> excludeChunks = new HashSet<>();
        UploadResponse uploadResponse = new UploadResponse();
        FileChunkResponse chunkResponse = resultData.getData();
        switch (chunkResponse.getUploadState()) {
            case completed:
                // 相当于秒传
                uploadResponse.setDocId(chunkResponse.getDocId());
                uploadResponse.setFileName(fileName);
                uploadResponse.setDocumentType(DocumentTypeUtil.getDocumentType(fileName));
                return ResultData.success(uploadResponse);
            case undone:
                // 未完成,续传
                chunkSize = Math.toIntExact(chunkResponse.getChunkSize());
                totalSize = chunkResponse.getTotalSize();
                totalChunks = chunkResponse.getTotalChunks();

                List<FileChunkDto> chunkDtos = chunkResponse.getChunks();
                if (totalChunks > 0 && totalChunks == chunkDtos.size()) {
                    // 分块上传完成,但未合并
                    return apiTemplate.postByUrl(getServiceUrl() + "/file/mergeFile?fileMd5=" + fileMd5 + "&fileName=" + fileName,
                            new ParameterizedTypeReference<ResultData<UploadResponse>>() {
                            });
                } else {
                    // 分块上传不完整
                    for (FileChunkDto chunk : chunkDtos) {
                        // 文件块从1开始,故需要减1
                        excludeChunks.add(chunk.getChunkNumber() - 1);
                    }
                }
                break;
            case none:
                // 新上传
                // 总大小
                totalSize = file.length();
                // 分块大小 50mb
                chunkSize = 50 * 1024 * 1024;
                // 总块数
                totalChunks = (int) Math.ceil(totalSize / Double.parseDouble(chunkSize + ""));
                break;
            default:
                return ResultData.fail("文件分块上传错误.");
        }

        try {
            boolean isSuccess = true;
            //已经读取的数据的大小
            int readSize = 0;
            int size = totalSize > chunkSize ? chunkSize : Math.toIntExact(totalSize);
            // 分块序号
            int index = 0;
            int len;
            byte[] buffer = new byte[size];
            try (InputStream stream = new FileInputStream(file)) {
                while ((len = stream.read(buffer, 0, size)) > 0) {
                    readSize += len;
                    // 当前文件块，从1开始
                    int chunkNumber = index + 1;
                    if (excludeChunks.contains(index)) {
                        index++;
                        buffer = new byte[size];
                        continue;
                    }
                    index++;

                    Resource resource = new ByteArrayResource(buffer) {
                        /**
                         * 覆写父类方法
                         * 如果不重写这个方法，并且文件有一定大小，那么服务端会出现异常
                         * {@code The multi-part request contained parameter data (excluding uploaded files) that exceeded}
                         */
                        @Override
                        public String getFilename() {
                            return FileUtils.getWithoutExtension(fileName) + chunkNumber + FileUtils.DOT + FileUtils.getExtension(fileName);
                        }
                    };

                    final MultiValueMap<String, Object> request = new LinkedMultiValueMap<>();
                    // 文件
                    request.add("file", resource);
                    // 当前块序号
                    request.add("chunkNumber", chunkNumber);
                    // 当前块大小
                    request.add("currentChunkSize", buffer.length);
                    // 块大小
                    request.add("chunkSize", chunkSize);
                    // 总大小
                    request.add("totalSize", totalSize);
                    // 总块数
                    request.add("totalChunks", totalChunks);
                    // 文件md5
                    request.add("fileMd5", fileMd5);

                    ResultData<UploadResponse> uploadResult = apiTemplate.uploadFileByUrl(getServiceUrl() + "/file/uploadChunk",
                            new ParameterizedTypeReference<ResultData<UploadResponse>>() {
                            }, request);
                    if (uploadResult.failed()) {
                        isSuccess = false;
                        LOG.error("文件MD5[{}]的分块[{}]上传失败, 错误消息: {}", fileMd5, chunkNumber, uploadResult.getMessage());
                    } else {
                        LOG.debug("文件MD5[{}]的分块[{}]上传成功 {}", fileMd5, chunkNumber, uploadResult.getSuccess());
                    }

                    //如果数据流的总长度减去已经读取的数据流的长度值小于每次读取数据流的设定的大小，那么就重新为buffer字节数组设定大小
                    if ((totalSize - readSize) < size) {
                        //这样可以避免最终得到的数据的结尾处多出多余的空值
                        size = (int) totalSize - readSize;
                        buffer = new byte[size];
                    } else {
                        buffer = new byte[size];
                    }
                }
            } catch (IOException e) {
                throw new ServiceException("数据流分块异常", e);
            }

            if (isSuccess) {
                return apiTemplate.postByUrl(getServiceUrl() + "/file/mergeFile?fileMd5=" + fileMd5 + "&fileName=" + fileName,
                        new ParameterizedTypeReference<ResultData<UploadResponse>>() {
                        });
            }
        } catch (Exception e) {
            LOG.error("文件分块上传异常", e);
        }
        return ResultData.fail("文件分块上传错误.");
    }

    /**
     * 文件分块上传
     * step1.文件分块
     * step2.分块上传
     * step3.合并分块
     *
     * @param fileName 文件名
     * @param stream   文件流
     * @return 返回上传结果
     */
    public ResultData<UploadResponse> uploadChunk(final String fileName, InputStream stream) throws IOException {
        ByteArrayOutputStream byteArrayOut = FileUtils.cloneInputStream(stream);
        if (Objects.isNull(byteArrayOut)) {
            throw new ServiceException("文件流不能为空.");
        }

        byte[] byteArray = byteArrayOut.toByteArray();
        byteArrayOut.close();
        byteArrayOut = null;
        // 获取文件md5
        StopWatch stopWatch = StopWatch.createStarted();
        String fileMd5 = MD5Utils.md5Stream(new ByteArrayInputStream(byteArray));
        stopWatch.stop();
        LOG.debug("MD5 耗时: {}", stopWatch.getTime());

        // 检查文件分块情况
        Map<String, String> params = new HashMap<>();
        params.put("fileMd5", fileMd5);
        ResultData<FileChunkResponse> resultData = apiTemplate.getByUrl(getServiceUrl() + "/file/checkChunk",
                new ParameterizedTypeReference<ResultData<FileChunkResponse>>() {
                }, params);
        if (resultData.failed()) {
            return ResultData.fail(resultData.getMessage());
        }

        // 总大小
        long totalSize;
        // 分块大小 50mb
        int chunkSize;
        // 总块数
        int totalChunks;

        Set<Integer> excludeChunks = new HashSet<>();
        UploadResponse uploadResponse = new UploadResponse();
        FileChunkResponse chunkResponse = resultData.getData();
        switch (chunkResponse.getUploadState()) {
            case completed:
                // 相当于秒传
                uploadResponse.setDocId(chunkResponse.getDocId());
                uploadResponse.setFileName(fileName);
                uploadResponse.setDocumentType(DocumentTypeUtil.getDocumentType(fileName));
                return ResultData.success(uploadResponse);
            case undone:
                // 未完成,续传
                chunkSize = Math.toIntExact(chunkResponse.getChunkSize());
                totalSize = chunkResponse.getTotalSize();
                totalChunks = chunkResponse.getTotalChunks();

                List<FileChunkDto> chunkDtos = chunkResponse.getChunks();
                if (totalChunks > 0 && totalChunks == chunkDtos.size()) {
                    // 分块上传完成,但未合并
                    return apiTemplate.postByUrl(getServiceUrl() + "/file/mergeFile?fileMd5=" + fileMd5 + "&fileName=" + fileName,
                            new ParameterizedTypeReference<ResultData<UploadResponse>>() {
                            });
                } else {
                    // 分块上传不完整
                    for (FileChunkDto chunk : chunkDtos) {
                        // 文件块从1开始,故需要减1
                        excludeChunks.add(chunk.getChunkNumber() - 1);
                    }
                }
                break;
            case none:
                // 新上传
                // 总大小
                totalSize = byteArray.length;
                // 分块大小 50mb
                chunkSize = 50 * 1024 * 1024;
                // 总块数
                totalChunks = (int) Math.ceil(totalSize / Double.parseDouble(chunkSize + ""));
                break;
            default:
                return ResultData.fail("文件分块上传错误.");
        }

        //
        byte[][] chunkData = new byte[totalChunks][];
        try {
            FileUtils.splitChunks(byteArray, totalSize, chunkSize, chunkData, excludeChunks);

            final int chunkSizeTemp = chunkSize;
            final long totalSizeTemp = totalSize;
            final int totalChunksTemp = totalChunks;

            // 异步上传
            CompletableFuture<ResultData<UploadResponse>> future = CompletableFuture.supplyAsync(() -> {
                // 当前文件块，从1开始
                int index = 1;
                for (byte[] data : chunkData) {
                    final int chunkNumber = index++;
                    if (data == null || data.length == 0) {
                        continue;
                    }
                    Resource resource = new ByteArrayResource(data) {
                        /**
                         * 覆写父类方法
                         * 如果不重写这个方法，并且文件有一定大小，那么服务端会出现异常
                         * {@code The multi-part request contained parameter data (excluding uploaded files) that exceeded}
                         */
                        @Override
                        public String getFilename() {
                            return FileUtils.getWithoutExtension(fileName) + chunkNumber + FileUtils.DOT + FileUtils.getExtension(fileName);
                        }
                    };

                    final MultiValueMap<String, Object> request = new LinkedMultiValueMap<>();
                    // 文件
                    request.add("file", resource);
                    // 当前块序号
                    request.add("chunkNumber", chunkNumber);
                    // 当前块大小
                    request.add("currentChunkSize", data.length);
                    // 块大小
                    request.add("chunkSize", chunkSizeTemp);
                    // 总大小
                    request.add("totalSize", totalSizeTemp);
                    // 总块数
                    request.add("totalChunks", totalChunksTemp);
                    // 文件md5
                    request.add("fileMd5", fileMd5);

                    ResultData<UploadResponse> uploadResult = apiTemplate.uploadFileByUrl(getServiceUrl() + "/file/uploadChunk",
                            new ParameterizedTypeReference<ResultData<UploadResponse>>() {
                            }, request);
                    if (uploadResult.failed()) {
                        LOG.error("文件MD5[{}]的分块[{}]上传失败, 错误消息: {}", fileMd5, chunkNumber, uploadResult.getMessage());
                    } else {
                        LOG.debug("文件MD5[{}]的分块[{}]上传成功 {}", fileMd5, chunkNumber, uploadResult.getSuccess());
                    }
                }
                return ResultData.success();
            }).handle((objectResultData, throwable) ->
                    apiTemplate.postByUrl(getServiceUrl() + "/file/mergeFile?fileMd5=" + fileMd5 + "&fileName=" + fileName,
                            new ParameterizedTypeReference<ResultData<UploadResponse>>() {
                            })
            );

            future.join();
            return future.get();
        } catch (Exception e) {
            LOG.error("文件分块上传异常", e);
        }
        return ResultData.success(uploadResponse);
    }

    /**
     * 上传一个文档
     *
     * @param fileName 文件名
     * @param data     文件数据
     *                 {@link FileUtils#readFileToByteArray(File)}
     * @return 文档信息
     */
    public UploadResponse uploadDocument(final String fileName, final byte[] data) {
        Resource resource = new ByteArrayResource(data) {
            /**
             * 覆写父类方法
             * 如果不重写这个方法，并且文件有一定大小，那么服务端会出现异常
             * {@code The multi-part request contained parameter data (excluding uploaded files) that exceeded}
             */
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        // 添加文件源
        params.add("file", resource);
        params.add("sys", ContextUtil.getAppCode());
        params.add("uploadUser", ContextUtil.getUserAccount());

        ResultData<UploadResponse> resultData = apiTemplate.uploadFileByUrl(getServiceUrl() + "/file/upload",
                new ParameterizedTypeReference<ResultData<UploadResponse>>() {
                }, params);
        if (resultData.failed()) {
            throw new ServiceException("通过EDM上传文件失败: " + resultData.getMessage());
        }
        return resultData.getData();
    }

    /**
     * 上传一个文档(如果是图像生成缩略图)
     *
     * @param stream   文档数据流
     *                 {@link FileUtils#openInputStream(File)}
     * @param fileName 文件名
     * @return 文档信息
     */
    public UploadResponse uploadDocument(final String fileName, InputStream stream) {
        Resource resource = new InputStreamResource(stream) {
            /**
             * 覆写父类方法
             * 如果不重写这个方法，并且文件有一定大小，那么服务端会出现异常
             * {@code The multi-part request contained parameter data (excluding uploaded files) that exceeded}
             */
            @Override
            public String getFilename() {
                return fileName;
            }

            /**
             * This implementation reads the entire InputStream to calculate the
             * content length. Subclasses will almost always be able to provide
             * a more optimal version of this, e.g. checking a File length.
             *
             * @see #getInputStream()
             */
            @Override
            public long contentLength() {
                return 1;
            }
        };

        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        // 添加文件源
        params.add("file", resource);
        params.add("sys", ContextUtil.getAppCode());
        params.add("uploadUser", ContextUtil.getUserAccount());

        ResultData<UploadResponse> resultData = apiTemplate.uploadFileByUrl(getServiceUrl() + "/file/upload",
                new ParameterizedTypeReference<ResultData<UploadResponse>>() {
                }, params);
        if (resultData.failed()) {
            throw new ServiceException("通过EDM上传文件失败: " + resultData.getMessage());
        }
        return resultData.getData();
    }

    /**
     * 获取一个文档(包含信息和数据)
     *
     * @param docId       文档Id
     * @param isThumbnail 是获取缩略图(默认宽150,高100)
     * @return 文档. {@link FileUtils#str2InputStream(String)} 或 {@link FileUtils#str2File(String, String)}
     */
    public DocumentResponse getDocument(String docId, boolean isThumbnail) {
        Map<String, String> params = new HashMap<>();
        // 添加文件源
        params.put("docId", docId);
        params.put("isThumbnail", String.valueOf(isThumbnail));

        ResultData<DocumentResponse> resultData = apiTemplate.getByUrl(getServiceUrl() + "/document/getDocument",
                new ParameterizedTypeReference<ResultData<DocumentResponse>>() {
                }, params);

        if (resultData.failed()) {
            throw new ServiceException("通过EDM上传文件失败: " + resultData.getMessage());
        }
        DocumentResponse response = resultData.getData();
        response.setData(FileUtils.decodeBase64(response.getBase64Data()));
        return response;
    }

    /**
     * 提交业务实体的文档信息
     *
     * @param entityId 绑定业务实体文档信息请求
     * @param docIds   绑定业务实体文档信息请求
     */
    public ResultData<String> bindBusinessDocuments(String entityId, Collection<String> docIds) {
        BindRequest request = new BindRequest();
        request.setEntityId(entityId);
        request.setDocumentIds(docIds);

        ResultData<String> resultData = apiTemplate.postByUrl(getServiceUrl() + "/document/bindBusinessDocuments",
                new ParameterizedTypeReference<ResultData<String>>() {
                }, request);
        return resultData;
    }

    /**
     * 删除业务实体的文档信息
     *
     * @param entityId 业务实体Id
     */
    public ResultData<String> deleteBusinessInfos(@NotBlank String entityId) {
        try {
            apiTemplate.postByUrl(getServiceUrl() + "/document/deleteBusinessInfos?entityId="+entityId,
                    new ParameterizedTypeReference<ResultData<String>>() {
                    }, null);
            return ResultData.success("OK");
        } catch (Exception e) {
            LogUtil.error("删除业务实体的文档信息失败", e);
            return ResultData.fail("删除业务实体的文档信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取一个文档(只包含文档信息,不含文档数据)
     *
     * @param docId 文档Id
     * @return 文档
     */
    public ResultData<DocumentResponse> getEntityDocumentInfo(@NotBlank String docId) {
        Map<String, String> params = new HashMap<>();
        params.put("docId", docId);

        ResultData<DocumentResponse> resultData = apiTemplate.getByUrl(getServiceUrl() + "/document/getEntityDocumentInfo",
                new ParameterizedTypeReference<ResultData<DocumentResponse>>() {
                }, params);
        return resultData;
    }

    /**
     * 获取业务实体的文档信息清单
     *
     * @param entityId 业务实体Id
     * @return 文档信息清单
     */
    public ResultData<List<DocumentResponse>> getEntityDocumentInfos(@NotBlank String entityId) {
        Map<String, String> params = new HashMap<>();
        params.put("entityId", entityId);

        ResultData<List<DocumentResponse>> resultData = apiTemplate.getByUrl(getServiceUrl() + "/document/getEntityDocumentInfos",
                new ParameterizedTypeReference<ResultData<List<DocumentResponse>>>() {
                }, params);
        return resultData;
    }

    /**
     * 转为pdf文件并存储
     * 目前支持Word,Powerpoint转为pdf文件
     *
     * @param docId    文档id,必须
     * @param markText 文档水印
     * @return 返回成功转为pdf存储的docId, 不能成功转为pdf的返回原docId
     */
    public ResultData<String> convert2PdfAndSave(@NotBlank String docId, String markText) {
        Map<String, String> params = new HashMap<>();
        params.put("docId", docId);
        if (StringUtils.isBlank(markText)) {
            markText = StringUtils.EMPTY;
        }
        params.put("markText", markText);

        ResultData<String> resultData = apiTemplate.getByUrl(getServiceUrl() + "/document/convert2PdfAndSave",
                new ParameterizedTypeReference<ResultData<String>>() {
                }, params);
        return resultData;
    }

}
