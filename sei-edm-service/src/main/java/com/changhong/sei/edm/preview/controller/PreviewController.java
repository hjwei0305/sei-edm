package com.changhong.sei.edm.preview.controller;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Objects;

@RefreshScope
@Controller
@Api(value = "文件在线预览", tags = "文件在线预览")
public class PreviewController {
    @Autowired
    private FileService fileService;
    //引入配置
    @Value("${sei.edm.base-url:none}")
    private String baseUrl;

//    @Autowired
//    private HttpServletRequest request;
//    @Autowired
//    private HttpServletResponse response;

    @GetMapping(value = "/preview")
    @ApiOperation("在线预览")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "docId", value = "附件id", required = true, paramType = "query"),
            @ApiImplicitParam(name = "markText", value = "水印内容", paramType = "query", example = "SEI6.0")
    })
    public String preview(@RequestParam("docId") String docId,
                          @RequestParam(name = "markText", required = false) String markText,
                          Model model) {
        model.addAttribute("baseUrl", baseUrl);

        model.addAttribute("docId", docId);
        if (StringUtils.isNotBlank(markText)) {
            model.addAttribute("markText", markText);
        }

        String view = "";
        DocumentResponse document = fileService.getDocumentInfo(docId);
        switch (document.getDocumentType()) {
            case Pdf:
            case Word:
            case Powerpoint:
                view = "preview/pdf.html";
                break;
            case Excel:
                view = "preview/html.html";
                break;
            case Image:
                view = "preview/image.html";
                break;
            case Text:
                view = "preview/txt.html";
                break;
            case Media:
                // http://f2ex.cn/plyr-html5-media-player/
                view = "preview/media.html";
                if (StringUtils.endsWithIgnoreCase(document.getFileName(), "flv")) {
                    view = "preview/flv.html";
                }
                break;
            case Compressed:

//                break;
            case Other:
            default:
                // 不支持
                view = "preview/nosupport.html";
        }

        return view;
    }

    /**
     * 读取已经转换好的文件
     *
     * @param docId docId
     */
    @GetMapping(value = "/preview/readFile")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "docId", value = "附件id", required = true, paramType = "query"),
            @ApiImplicitParam(name = "markText", value = "水印内容", paramType = "query", example = "SEI6.0")
    })
    public ResponseEntity<byte[]> readFile(@RequestParam(name = "docId") String docId,
                                           @RequestParam(name = "markText", required = false) String markText,
                                           HttpServletResponse response) {
        response.setHeader("Content-type", "text/html;charset=UTF-8");
        response.setCharacterEncoding("utf-8");

        if (StringUtils.isBlank(docId)) {
            LogUtil.warn("fileName is blank");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        DocumentResponse document = fileService.getDocument(docId);
        if (Objects.isNull(document)) {
            LogUtil.error("file is not found");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (StringUtils.isNotBlank(markText)
                && !StringUtils.equalsAnyIgnoreCase(markText, "null")) {
            // 水印内容
            document.setMarkText(markText);
        }

        // 文件预览转换
        ResultData<DocumentResponse> resultData = PreviewServiceFactory.getPreviewDocument(document);
        if (resultData.failed()) {
            LogUtil.error("file convert error");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        DocumentResponse documentResponse = resultData.getData();
        try {
            //判断文件类型
            String mimeType = URLConnection.guessContentTypeFromName(documentResponse.getFileName());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            response.setContentType(mimeType);

            //设置文件响应大小
            response.setContentLengthLong(documentResponse.getSize());

            byte[] bytes = documentResponse.getData();
            response.getOutputStream().write(bytes);
            return new ResponseEntity<>(bytes, HttpStatus.OK);
        } catch (IOException e) {
            LogUtil.error("readFile error: " + e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
    }

}
