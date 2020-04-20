package com.changhong.sei.edm.sdk;

import com.changhong.sei.apitemplate.ApiTemplate;
import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.dto.BindRequest;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.dto.UploadResponse;
import com.changhong.sei.exception.ServiceException;
import com.changhong.sei.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.validation.constraints.NotBlank;
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
public class DocumentManager {

    private static final String appCode = "edm-service";
    @Autowired
    private ApiTemplate apiTemplate;

    /**
     * 上传一个文档
     *
     * @param fileName 文件名
     * @param data     文件数据
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

        ResultData<UploadResponse> resultData = apiTemplate.uploadFileByAppModuleCode(appCode, "/file/upload",
                new ParameterizedTypeReference<ResultData<UploadResponse>>() {
                }, params);
//        ResultData<UploadResponse> resultData = apiTemplate.uploadFileByUrl("http://localhost:8080/edm/file/upload",
//                new ParameterizedTypeReference<ResultData<UploadResponse>>() {
//                }, params);
        if (resultData.failed()) {
            throw new ServiceException("通过EDM上传文件失败: " + resultData.getMessage());
        }
        return resultData.getData();
    }

    /**
     * 上传一个文档(如果是图像生成缩略图)
     *
     * @param stream   文档数据流
     * @param fileName 文件名
     * @return 文档信息
     */
    UploadResponse uploadDocument(String fileName, InputStream stream) {
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

        ResultData<UploadResponse> resultData = apiTemplate.uploadFileByAppModuleCode(appCode, "/file/upload",
                new ParameterizedTypeReference<ResultData<UploadResponse>>() {
                }, params);
//        ResultData<UploadResponse> resultData = apiTemplate.uploadFileByUrl("http://localhost:8080/edm/file/upload",
//                new ParameterizedTypeReference<ResultData<UploadResponse>>() {
//                }, params);
        if (resultData.failed()) {
            throw new ServiceException("通过EDM上传文件失败: " + resultData.getMessage());
        }
        return resultData.getData();
    }

//    /**
//     * 上传扫描件方法
//     * （包含上传和保存关联关系）
//     *
//     * @param scanVo
//     * @return
//     */
//    DocumentInfo uploadScan(UploadScanVo scanVo);

    /**
     * 获取一个文档(包含信息和数据)
     *
     * @param docId       文档Id
     * @param isThumbnail 是获取缩略图
     * @return 文档
     */
    DocumentResponse getDocument(String docId, boolean isThumbnail) {
        Map<String, String> params = new HashMap<>();
        // 添加文件源
        params.put("isThumbnail", String.valueOf(isThumbnail));

        ResultData<DocumentResponse> resultData = apiTemplate.getByAppModuleCode(appCode, "/document/" + docId,
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
    void bindBusinessDocuments(String entityId, Collection<String> docIds) {
        BindRequest request = new BindRequest();
        request.setEntityId(entityId);
        request.setDocumentIds(docIds);

        ResultData<DocumentResponse> resultData = apiTemplate.postByAppModuleCode(appCode, "/document/bindBusinessDocuments",
                new ParameterizedTypeReference<ResultData<DocumentResponse>>() {
                }, request);
        if (resultData.failed()) {
            throw new ServiceException("通过EDM上传文件失败: " + resultData.getMessage());
        }
    }

    /**
     * 删除业务实体的文档信息
     *
     * @param entityId 业务实体Id
     */
    void deleteBusinessInfos(@NotBlank String entityId) {

    }

    /**
     * 获取一个文档(只包含文档信息,不含文档数据)
     *
     * @param docId 文档Id
     * @return 文档
     */
    DocumentResponse getEntityDocumentInfo(@NotBlank String docId) {
        return null;

    }

    /**
     * 获取业务实体的文档信息清单
     *
     * @param entityId 业务实体Id
     * @return 文档信息清单
     */
    List<DocumentResponse> getEntityDocumentInfos(@NotBlank String entityId) {

        return null;
    }
}
