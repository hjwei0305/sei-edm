package com.changhong.sei.edm.api;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.dto.BindRequest;
import com.changhong.sei.edm.dto.DocumentResponse;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-05 16:16
 */
@Validated
@RequestMapping(name = "edm-service", path = "document", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface DocumentApi {
//    /**
//     * 上传一个文档(如果是图像生成缩略图)
//     *
//     * @param document 文档
//     * @return 文档信息
//     */
//    @PostMapping(path = "uploadDocument")
//    @ApiOperation(value = "上传一个文档", notes = "上传一个文档")
//    DocumentResponse uploadDocument(Document document);
//
//    /**
//     * 上传一个文档(如果是图像生成缩略图)
//     *
//     * @param base64Str   文档数据流
//     * @param fileName 文件名
//     * @return 文档信息
//     */
//    @POST
//    @Path("uploadDocumentByName")
//    @ApiOperation(value = "上传一个文档", notes = "上传一个文档")
//    DocumentInfo uploadDocument(String base64Str, @RequestParam("fileName") String fileName);
//    DocumentInfo uploadDocument(InputStream stream, @RequestParam("fileName") String fileName);
//
//    /**
//     * 上传扫描件方法
//     * （包含上传和保存关联关系）
//     *
//     * @param scanVo
//     * @return
//     */
//    @POST
//    @Path("uploadScan")
//    @ApiOperation(value = "上传一个文档", notes = "上传一个文档")
//    DocumentInfo uploadScan(UploadScanVo scanVo);

    /**
     * 获取一个文档(包含信息和数据)
     *
     * @param docId          文档Id
     * @param isThumbnail 是获取缩略图
     * @return 文档
     */
    @GetMapping("{docId}")
    @ApiOperation(value = "获取一个文档(包含信息和数据)", notes = "获取一个文档(包含信息和数据)")
    ResultData<DocumentResponse> getDocument(@PathVariable("docId") String docId, @RequestParam(name = "isThumbnail", required = false) boolean isThumbnail);

    /**
     * 提交业务实体的文档信息
     *
     * @param request 绑定业务实体文档信息请求
     */
    @PostMapping("bindBusinessDocuments")
    @ApiOperation(value = "提交业务实体的文档信息", notes = "提交业务实体的文档信息")
    ResultData<String> bindBusinessDocuments(@RequestBody BindRequest request);

    /**
     * 删除业务实体的文档信息
     *
     * @param entityId 业务实体Id
     */
    @PostMapping("deleteBusinessInfos")
    @ApiOperation(value = "删除业务实体的文档信息", notes = "删除业务实体的文档信息")
    ResultData<String> deleteBusinessInfos(@RequestParam("entityId") @NotBlank String entityId);

    /**
     * 获取一个文档(只包含文档信息,不含文档数据)
     *
     * @param docId 文档Id
     * @return 文档
     */
    @GetMapping("getEntityDocumentInfo")
    @ApiOperation(value = "获取一个文档(只包含文档信息,不含文档数据)", notes = "获取一个文档(只包含文档信息,不含文档数据)")
    ResultData<DocumentResponse> getEntityDocumentInfo(@RequestParam("docId") @NotBlank String docId);

    /**
     * 获取业务实体的文档信息清单
     *
     * @param entityId 业务实体Id
     * @return 文档信息清单
     */
    @GetMapping("getEntityDocumentInfos")
    @ApiOperation(value = "获取业务实体的文档信息清单", notes = "获取业务实体的文档信息清单")
    ResultData<List<DocumentResponse>> getEntityDocumentInfos(@RequestParam("entityId") @NotBlank String entityId);
}
