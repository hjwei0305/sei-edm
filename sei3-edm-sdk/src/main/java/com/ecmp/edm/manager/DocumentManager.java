package com.ecmp.edm.manager;

import com.ecmp.edm.common.HttpClientResult;
import com.ecmp.edm.common.HttpClientUtils;
import com.ecmp.edm.entity.Document;
import com.ecmp.edm.entity.DocumentInfo;
import com.ecmp.util.DateUtils;
import com.ecmp.util.FileUtils;
import com.ecmp.util.JsonUtils;
import com.ecmp.vo.ResponseData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ParameterizedTypeReference;

import java.io.InputStream;
import java.util.*;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：文档管理器
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017-07-11 16:47      王锦光(wangj)                新建
 * <p/>
 * *************************************************************************************************
 */
@SuppressWarnings("rawtypes")
public class DocumentManager implements IDocumentManager, ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentManager.class);

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    private String getServiceUrl() {
        String host = context.getEnvironment().getProperty("sei.edm.service.url");
        if (org.springframework.util.StringUtils.isEmpty(host)) {
            throw new IllegalArgumentException("EDM服务地址未配置[sei.edm.service.url]");
        }
        return host;
    }

    /**
     * 上传一个文档(如果是图像生成缩略图)
     *
     * @param document 文档
     * @return 文档信息
     */
    @Override
    public DocumentInfo uploadDocument(Document document) {
        if (Objects.isNull(document)) {
            throw new IllegalArgumentException("document is null");
        }
        if (Objects.isNull(document.getStream())) {
            throw new IllegalArgumentException("document is error");
        }

        DocumentInfo info = document.getInfo();
        if (Objects.isNull(info)) {
            throw new IllegalArgumentException("document is error");
        }

        String url = getServiceUrl() + "/file/upload?sys=" + (StringUtils.isBlank(info.getAppModule()) ? "sei" : info.getAppModule());
        try {
            HttpClientResult result = HttpClientUtils.upload(url, StringUtils.isBlank(info.getFileName()) ? "temp" : info.getFileName(), document.getStream());
            if (result.getCode() == 200) {
                Map map = JsonUtils.fromJson(result.getContent(), Map.class);
                Map mapData = (Map) map.get("data");
                if (Objects.nonNull(mapData)) {
                    info.setId((String) mapData.get("docId"));
                }
            }
        } catch (Exception e) {
            LOG.error("附件上传异常", e);
        }
        return info;
    }

    /**
     * 上传一个文档(如果是图像生成缩略图)
     *
     * @param stream   文档数据流
     * @param fileName 文件名
     * @return 文档信息
     */
    @Override
    public DocumentInfo uploadDocument(InputStream stream, String fileName) {
        if (Objects.isNull(fileName)) {
            throw new IllegalArgumentException("fileName is null");
        }
        if (Objects.isNull(stream)) {
            throw new IllegalArgumentException("stream is null");
        }

        DocumentInfo info = new DocumentInfo();
        info.setFileName(fileName);

        String url = getServiceUrl() + "/file/upload?sys=SEI3";
        try {
            HttpClientResult result = HttpClientUtils.upload(url, fileName, stream);
            if (result.getCode() == 200) {
                Map map = JsonUtils.fromJson(result.getContent(), Map.class);
                Map mapData = (Map) map.get("data");
                if (Objects.nonNull(mapData)) {
                    info.setId((String) mapData.get("docId"));
                }
            }
        } catch (Exception e) {
            LOG.error("附件上传异常", e);
        }
        return info;
    }

    /**
     * 获取一个文档(包含信息和数据)
     *
     * @param docId       文档Id
     * @param isThumbnail 是获取缩略图
     * @return 文档
     */
    @Override
    public Document getDocument(String docId, boolean isThumbnail) {
        Document document = null;
        String url = getServiceUrl() + "/document/getDocument";
        Map<String, String> params = new HashMap<>();
        // 添加文件源
        params.put("docId", docId);
        params.put("isThumbnail", String.valueOf(isThumbnail));
        try {
            HttpClientResult result = HttpClientUtils.doGet(url, params);
            if (200 == result.getCode()) {
                Map map = JsonUtils.fromJson(result.getContent(), Map.class);
                Map mapData = (Map) map.get("data");
                if (Objects.nonNull(mapData)) {
                    DocumentInfo info = new DocumentInfo();
                    info.setId((String) mapData.get("docId"));
                    info.setFileName((String) mapData.get("fileName"));
                    info.setAppModule((String) mapData.get("system"));
                    info.setUploadUserName((String) mapData.get("uploadUser"));
                    Object temp = mapData.get("base64Data");
                    if (Objects.nonNull(temp)) {
                        try {
                            document = new Document(info, FileUtils.str2InputStream((String) temp));
                        } catch (Exception ignored) {
                        }
                    }
                    temp = mapData.get("uploadedTime");
                    if (Objects.nonNull(temp)) {
                        try {
                            info.setUploadedTime(DateUtils.parseTime((String) temp, "yyyy-MM-dd HH:mm:ss"));
                        } catch (Exception ignored) {
                        }
                    }
                    temp = mapData.get("size");
                    if (Objects.nonNull(temp)) {
                        try {
                            info.setSize(Long.valueOf(String.valueOf(temp)));
                        } catch (Exception ignored) {
                        }
                    }
                    mapData = null;
                }
                map = null;
            }
        } catch (Exception e) {
            LOG.error("获取文档异常", e);
        }
        return document;
    }

    /**
     * 获取一个文档(包含信息和数据)
     *
     * @param docId 文档Id
     * @return 文档
     */
    @Override
    public Document getDocument(String docId) {
        return getDocument(docId, false);
    }

    /**
     * 提交业务实体的文档信息
     *
     * @param entityId    业务实体Id
     * @param documentIds 文档Id清单
     */
    @Override
    public void submitBusinessInfos(String entityId, Collection<String> documentIds) {
        String url = getServiceUrl() + "/document/bindBusinessDocuments";
        Map<String, Object> params = new HashMap<>();
        // 添加文件源
        params.put("entityId", entityId);
        params.put("documentIds", documentIds);
        try {
            HttpClientResult result = HttpClientUtils.doPostJson(url, JsonUtils.toJson(params));
            if (200 == result.getCode()) {
                LOG.info("提交业务实体的文档信息成功");
            } else {
                LOG.error("提交业务实体的文档信息失败");
            }
        } catch (Exception e) {
            LOG.error("提交业务实体的文档信息异常", e);
        }
    }

    /**
     * 删除业务实体的文档信息
     *
     * @param entityId 业务实体Id
     */
    @Override
    public void deleteBusinessInfos(String entityId) {
        String url = getServiceUrl() + "/document/deleteBusinessInfos?entityId="+entityId;
        try {
            HttpClientResult result = HttpClientUtils.doPost(url, null);
            if (200 == result.getCode()) {
                LOG.info("删除业务实体的文档信息成功");
            } else {
                LOG.error("删除业务实体的文档信息失败");
            }
        } catch (Exception e) {
            LOG.error("删除业务实体的文档信息异常", e);
        }
    }

    /**
     * 获取业务实体的文档信息清单
     *
     * @param entityId 业务实体Id
     * @return 文档信息清单
     */
    @Override
    public List<DocumentInfo> getEntityDocumentInfos(String entityId) {
        List<DocumentInfo> infos = new ArrayList<>();
        String url = getServiceUrl() + "/document/getEntityDocumentInfos";
        Map<String, String> params = new HashMap<>();
        // 添加文件源
        params.put("entityId", entityId);
        try {
            HttpClientResult result = HttpClientUtils.doGet(url, params);
            if (200 == result.getCode()) {
                Map map = JsonUtils.fromJson(result.getContent(), Map.class);
                List<Map> list = (List) map.get("data");
                if (list != null && list.size() > 0) {
                    DocumentInfo info;
                    for (Map mapData : list) {
                        info = new DocumentInfo();
                        info.setId((String) mapData.get("docId"));
                        info.setFileName((String) mapData.get("fileName"));
                        info.setAppModule((String) mapData.get("system"));
                        info.setUploadUserName((String) mapData.get("uploadUser"));

                        Object temp = mapData.get("uploadedTime");
                        if (Objects.nonNull(temp)) {
                            try {
                                info.setUploadedTime(DateUtils.parseTime((String) temp, "yyyy-MM-dd HH:mm:ss"));
                            } catch (Exception ignored) {
                            }
                        }
                        temp = mapData.get("size");
                        if (Objects.nonNull(temp)) {
                            try {
                                info.setSize(Long.valueOf(String.valueOf(temp)));
                            } catch (Exception ignored) {
                            }
                        }
                        infos.add(info);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("获取文档异常", e);
        }
        return infos;
    }

    /**
     * 转为pdf文件并存储
     * 目前支持Word,Powerpoint转为pdf文件
     *
     * @param docId    文档id,必须
     * @param markText 文档水印
     * @return 返回成功转为pdf存储的docId, 不能成功转为pdf的返回原docId
     */
    @Override
    public ResponseData<String> convert2PdfAndSave(String docId, String markText) {
        Map<String, String> params = new HashMap<>();
        params.put("docId", docId);
        if (StringUtils.isBlank(markText)) {
            markText = StringUtils.EMPTY;
        }
        params.put("markText", markText);

        ResponseData<String> resultData;
        String url = getServiceUrl() + "/document/convert2PdfAndSave";
        try {
            HttpClientResult result = HttpClientUtils.doGet(url, params);
            if (200 == result.getCode()) {
                Map map = JsonUtils.fromJson(result.getContent(), Map.class);
                docId = (String) map.get("data");
                resultData = ResponseData.operationSuccessWithData(docId);
            } else {
                resultData = ResponseData.operationFailure(result.getContent());
            }
        } catch (Exception e) {
            LOG.error("转为pdf文件存储异常", e);
            resultData = ResponseData.operationFailure("转为pdf文件存储异常");
        }
        return resultData;
    }
}
