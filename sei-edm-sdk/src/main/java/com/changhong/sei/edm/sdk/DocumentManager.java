package com.changhong.sei.edm.sdk;

import com.changhong.sei.apitemplate.ApiTemplate;
import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.dto.BindRequest;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.dto.UploadResponse;
import com.changhong.sei.exception.ServiceException;
import com.changhong.sei.util.FileUtils;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return context.getEnvironment().getProperty("sei.edm.service.url", "http://10.4.208.86:20007/edm-service");
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
            public long contentLength() throws IOException {
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
            Map<String, String> params = new HashMap<>();
            params.put("entityId", entityId);
            apiTemplate.postByUrl(getServiceUrl() + "/document/deleteBusinessInfos",
                    new ParameterizedTypeReference<ResultData<String>>() {
                    }, params);
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
}
