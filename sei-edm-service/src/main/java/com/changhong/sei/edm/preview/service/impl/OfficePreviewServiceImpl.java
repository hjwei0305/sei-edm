package com.changhong.sei.edm.preview.service.impl;

import com.beust.jcommander.internal.Lists;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.core.util.HttpUtils;
import com.changhong.sei.core.util.JsonUtils;
import com.changhong.sei.edm.common.util.MD5Utils;
import com.changhong.sei.edm.dto.DocumentDto;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.dto.DocumentType;
import com.changhong.sei.edm.dto.UploadResponse;
import com.changhong.sei.edm.file.service.FileConverterService;
import com.changhong.sei.edm.file.service.FileService;
import com.changhong.sei.edm.manager.service.DocumentService;
import com.changhong.sei.edm.preview.service.PreviewService;
import com.changhong.sei.util.FileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 实现功能：Office预览服务
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-08 10:38
 */
@Service
public class OfficePreviewServiceImpl implements PreviewService {
    public static final int CACHE = 10 * 1024;
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfPreviewServiceImpl.class);

    @Autowired
    private FileConverterService fileConvertService;
    @Autowired
    private FileService fileService;
    @Autowired
    private DocumentService documentService;

    @Value("${sei.edm.preview.changhong.enable:false}")
    private boolean enable;

    @Value("${sei.edm.preview.changhong.token:none}")
    private String token;
    // https://tccps.changhong.com/dcs-test
    @Value("${sei.edm.preview.changhong.url:none}")
    private String requestUrl;
    //edm服务基地址   https://tecmp.changhong.com/api-gateway/edm-service
    @Value("${sei.edm.preview.changhong.edm-url:none}")
    private String baseUrl;

    public static final String DOT = ".";
    public static final String FILE_PATH;

    static {
        FILE_PATH = System.getProperty("user.dir").concat(File.separator).concat("ias_tmp").concat(File.separator);
        File file = new File(FILE_PATH);
        file.mkdirs();
        LogUtil.bizLog("文件临时目录: {}", FILE_PATH);
    }

    /**
     * 将文档转为预览文档
     *
     * @param document 需要转换的文件
     * @return 返回预览文档
     */
    @Override
    public ResultData<DocumentResponse> preview(DocumentDto document) {
        if (Objects.isNull(document)) {
            return ResultData.fail("document不能为空.");
        }

        String docId = document.getDocId();
        ResultData<DocumentResponse> result;
        if (enable && (DocumentType.Excel != document.getDocumentType())) {
            if (StringUtils.equals("none", requestUrl)) {
                return ResultData.fail("集团文档转换服务地址未配置.");
            }
            if (StringUtils.equals("none", token)) {
                return ResultData.fail("集团文档转换服务token未配置.");
            }
            String url = requestUrl.concat("/request/api/v1/convert/to_pdf?source_url=");
            Map<String, String> headers = new HashMap<>(7);
            headers.put("X-Stargate-App-Token", token);
            File file;
            try {
                String sourceUrl = URLEncoder.encode(baseUrl.concat("/preview/readFile/").concat(docId).concat(".").concat(FileUtils.getExtension(document.getFileName())), HttpUtils.CHARSET);
                // 发起转换
                String json = HttpUtils.sendPost(url.concat(sourceUrl), "", headers);
                if (StringUtils.isNotBlank(json)) {
                    JsonNode jsonNode = JsonUtils.parse(json);
                    if ("0".equals(jsonNode.get("err_code").asText(""))) {
                        JsonNode data = jsonNode.get("data");
                        if (Objects.nonNull(data)) {
                            // 得到转换后的pdf地址
                            String pdfUrl = data.get("pdf_url").asText("");
                            if (StringUtils.isNotBlank(pdfUrl)) {
                                file = new File(FILE_PATH.concat(docId).concat(DOT).concat("pdf"));

                                HttpClient client = HttpUtils.createClient(pdfUrl);
                                HttpGet httpget = new HttpGet(pdfUrl);
                                for (Map.Entry<String, String> entry : headers.entrySet()) {
                                    httpget.addHeader(entry.getKey(), entry.getValue());
                                }
                                HttpResponse response = client.execute(httpget);

                                HttpEntity entity = response.getEntity();
                                try (InputStream is = entity.getContent(); FileOutputStream fileOut = new FileOutputStream(file);) {
                                    byte[] buffer = new byte[CACHE];
                                    int ch;
                                    while ((ch = is.read(buffer)) != -1) {
                                        fileOut.write(buffer, 0, ch);
                                    }
                                    fileOut.flush();

                                    byte[] fileData = FileUtils.readFileToByteArray(file);
                                    // 异步将转换后的文件持久化,并绑定关系
                                    CompletableFuture.runAsync(() -> {
                                        try (InputStream inputStream = new ByteArrayInputStream(fileData)) {
                                            DocumentDto dto = new DocumentDto();
                                            dto.setData(fileData);
                                            // 计算文件MD5
                                            dto.setFileMd5(MD5Utils.md5Stream(inputStream));
                                            dto.setFileName(file.getName());
                                            ResultData<UploadResponse> uploadResult = fileService.uploadDocument(dto);
                                            if (uploadResult.successful()) {
                                                documentService.bindBusinessDocuments(docId, Lists.newArrayList(uploadResult.getData().getDocId()));
                                            }
                                        } catch (Exception e) {
                                            LOGGER.error("异步保存文件转换关系异常", e);
                                        }
                                    });

                                    DocumentResponse documentResponse = new DocumentResponse();
                                    documentResponse.setFileName(file.getName());
                                    documentResponse.setData(fileData);
                                    documentResponse.setSize((long) fileData.length);
                                    return ResultData.success(documentResponse);
                                } catch (Exception e) {
                                    LOGGER.error(document.getFileName() + "-调用集团文档转换服务异常", e);
                                } finally {
                                    file.delete();
                                }
                            }
                        }
                    }
                }
                result = ResultData.fail(document.getFileName() + "-调用集团文档转换服务异常");
            } catch (Exception e) {
                LOGGER.error("调用集团文档转换服务异常", e);
                result = ResultData.fail(document.getFileName() + "-调用集团文档转换服务异常");
            }
        } else {
            try (InputStream inputStream = new ByteArrayInputStream(document.getData())) {
                result = fileConvertService.convertInputStream(inputStream, document.getFileName(), document.getMarkText());
                // 水印可能跟个人有关,故不保存
                if (result.successful() && StringUtils.isBlank(document.getMarkText())) {
                    DocumentResponse documentResponse = result.getData();

                    CompletableFuture.runAsync(() -> {
                        try (InputStream is = new ByteArrayInputStream(documentResponse.getData())) {
                            // 异步将转换后的文件持久化,并绑定关系
                            DocumentDto dto = new DocumentDto();
                            dto.setData(documentResponse.getData());
                            // 计算文件MD5
                            dto.setFileMd5(MD5Utils.md5Stream(is));
                            dto.setFileName(documentResponse.getFileName());
                            ResultData<UploadResponse> uploadResult = fileService.uploadDocument(dto);
                            if (uploadResult.successful()) {
                                documentService.bindBusinessDocuments(docId, Lists.newArrayList(uploadResult.getData().getDocId()));
                            }
                        } catch (IOException e) {
                            LOGGER.error("异步保存文件转换关系异常", e);
                        }
                    });
                }
            } catch (Exception e) {
                LOGGER.error("office文档转为预览文档异常", e);
                result = ResultData.fail(document.getFileName() + "-转为预览文档异常");
            }
        }
        return result;
    }

}
