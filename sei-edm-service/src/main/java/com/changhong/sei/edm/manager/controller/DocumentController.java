package com.changhong.sei.edm.manager.controller;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.dto.serach.SearchFilter;
import com.changhong.sei.edm.api.DocumentApi;
import com.changhong.sei.edm.dto.BindRequest;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.file.service.FileConverterService;
import com.changhong.sei.edm.file.service.FileService;
import com.changhong.sei.edm.manager.entity.Document;
import com.changhong.sei.edm.manager.service.DocumentService;
import io.swagger.annotations.Api;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-05 16:15
 */
@RestController
@Api(value = "DocumentApi", tags = "业务文档服务")
public class DocumentController implements DocumentApi {

    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private DocumentService service;
    @Autowired
    private FileService fileService;
    @Autowired
    private FileConverterService fileConverterService;

    /**
     * 获取一个文档(包含信息和数据)
     *
     * @param docId       文档Id
     * @param isThumbnail 是获取缩略图
     * @return 文档
     */
    @Override
    public ResultData<DocumentResponse> getDocument(@NotBlank String docId, boolean isThumbnail) {
        DocumentResponse response;
        if (isThumbnail) {
            response = fileService.getThumbnail(docId, 150, 100);
        } else {
            response = fileService.getDocument(docId);
        }
        if (Objects.nonNull(response)) {
            // 返回Base64编码过的字节数组字符串
            response.setBase64Data(Base64.encodeBase64String(response.getData()));
            return ResultData.success(response);
        }
        return ResultData.fail("没有找到对应的文档信息清单");
    }

    /**
     * 获取一个文档(包含信息和数据)
     *
     * @param docId 文档Id
     * @return 文档
     */
    public ResultData<DocumentResponse> getDocument(@NotBlank String docId) {
        Document document = service.findByProperty(Document.FIELD_DOC_ID, docId);
        if (Objects.nonNull(document)) {
            DocumentResponse response = new DocumentResponse();
            modelMapper.map(document, response);
            return ResultData.success(response);
        }
        return ResultData.fail("没有找到对应的文档信息清单");
    }

    /**
     * 提交业务实体的文档信息
     *
     * @param request 绑定业务实体文档信息请求
     */
    @Override
    public ResultData<String> bindBusinessDocuments(BindRequest request) {
        String entityId = request.getEntityId();
        Collection<String> documentIds = request.getDocumentIds();
        return service.bindBusinessDocuments(entityId, documentIds);
    }

    /**
     * 删除业务实体的文档信息
     *
     * @param entityId 业务实体Id
     */
    @Override
    public ResultData<String> deleteBusinessInfos(@NotBlank String entityId) {
        return service.unbindBusinessDocuments(entityId);
    }

    /**
     * 获取一个文档(只包含文档信息,不含文档数据)
     *
     * @param docId 文档Id
     * @return 文档
     */
    @Override
    public ResultData<DocumentResponse> getEntityDocumentInfo(@NotBlank String docId) {
        DocumentResponse response = new DocumentResponse();
        Document document = service.findByProperty(Document.FIELD_DOC_ID, docId);
        if (Objects.nonNull(document)) {
            modelMapper.map(document, response);
        }
        return ResultData.success(response);
//        return ResultData.fail("没有找到对应的文档信息清单");
    }

    /**
     * 获取文档清单(只包含文档信息,不含文档数据)
     *
     * @param docIds 文档Id清单
     * @return 文档
     */
    @Override
    public ResultData<List<DocumentResponse>> getEntityDocumentInfoList(List<String> docIds) {
        List<DocumentResponse> responseList = new ArrayList<>();
        List<Document> documentList = service.findByFilter(new SearchFilter(Document.FIELD_DOC_ID, docIds, SearchFilter.Operator.IN));
        if (CollectionUtils.isNotEmpty(documentList)) {
            responseList = documentList.parallelStream().map(document -> {
                DocumentResponse response = new DocumentResponse();
                if (Objects.nonNull(document)) {
                    modelMapper.map(document, response);
                }
                return response;
            }).collect(Collectors.toList());
        }
        return ResultData.success(responseList);
    }

    /**
     * 获取业务实体的文档信息清单
     *
     * @param entityId 业务实体Id
     * @return 文档信息清单
     */
    @Override
    public ResultData<List<DocumentResponse>> getEntityDocumentInfos(@NotBlank String entityId) {
        List<DocumentResponse> result = new ArrayList<>();
        List<Document> documents = service.getDocumentsByEntityId(entityId);
        if (CollectionUtils.isNotEmpty(documents)) {
            DocumentResponse response;
            for (Document document : documents) {
                response = new DocumentResponse();
                modelMapper.map(document, response);
                result.add(response);
            }
        }
        return ResultData.success(result);
//        return ResultData.fail("没有找到对应的文档信息清单");
    }

    /**
     * 转为pdf文件并存储
     * 目前支持Word,Powerpoint转为pdf文件
     *
     * @param docId    文档id
     * @param markText 文档水印
     * @return 返回成功转为pdf存储的docId, 不能成功转为pdf的返回原docId
     */
    @Override
    public ResultData<String> convert2PdfAndSave(String docId, String markText) {
        return fileConverterService.convert2PdfAndSave(docId, markText);
    }
}
