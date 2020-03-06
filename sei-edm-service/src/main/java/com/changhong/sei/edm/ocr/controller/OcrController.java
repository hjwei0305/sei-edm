package com.changhong.sei.edm.ocr.controller;

import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.file.service.FileService;
import com.changhong.sei.edm.preview.service.PreviewServiceFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Objects;

@Controller
@Api(value = "文件在线预览", tags = "文件在线预览")
public class OcrController {
    @Autowired
    private FileService fileService;

//    @ApiOperation("文件上传")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "sys", value = "来源系统"),
//            @ApiImplicitParam(name = "file", value = "文件", required = true)
//    })
////    @PostMapping(value = "/upload", params = )
//    @ResponseBody
//    public ResultData<String> upload(@RequestParam("file") MultipartFile[] files,
//                                     @RequestParam(value = "sys", required = false) String sys) throws IOException {
//        if (StringUtils.isBlank(sys)) {
//            sys = ContextUtil.getAppCode();
//        }
//        boolean first = true;
//        StringBuilder docIds = new StringBuilder();
//        ResultData<String> resultData;
//        for (MultipartFile file : files) {
//            resultData = fileService.uploadDocument(file.getOriginalFilename(), sys, file.getBytes());
//            if (resultData.successful()) {
//                docIds.append(resultData.getData());
//                if (!first) {
//                    docIds.append(",");
//                }
//                first = false;
//            }
//        }
//        return ResultData.success(docIds.toString());
//    }

}
