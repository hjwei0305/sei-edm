package com.changhong.sei.edm.ocr.controller;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.dto.OcrType;
import com.changhong.sei.edm.file.service.FileService;
import com.changhong.sei.edm.ocr.service.CharacterReaderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Objects;

@Controller
@Api(value = "文件识别", tags = "文件识别")
public class OcrController {
    @Autowired
    private FileService fileService;
    @Autowired
    private CharacterReaderService characterReaderService;

    @ApiOperation("文件识别")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "docId", value = "文档Id"),
            @ApiImplicitParam(name = "ocr", dataTypeClass = OcrType.class, value = "ocr识别类型: None, Barcode, InvoiceQr, Qr ")
    })
    @PostMapping(value = "/ocr")
    @ResponseBody
    public ResultData<String> upload(@RequestParam(value = "docId") String docId,
                                     @RequestParam(value = "ocr") String ocr) {
        ResultData<String> result;
        OcrType ocrType = Enum.valueOf(OcrType.class, ocr);
        DocumentResponse docResponse = fileService.getDocument(docId);
        if (Objects.nonNull(docResponse)) {
            result = characterReaderService.read(docResponse.getDocumentType(), ocrType, docResponse.getData());
        } else {
            result = ResultData.fail("未找到文档[" + docId + "]");
        }
        return result;
    }

}
