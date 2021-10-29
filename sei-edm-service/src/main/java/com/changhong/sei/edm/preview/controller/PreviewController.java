package com.changhong.sei.edm.preview.controller;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.common.FileConstants;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.file.service.FileService;
import com.changhong.sei.edm.preview.service.PreviewServiceFactory;
import com.changhong.sei.util.FileUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.Objects;

@RefreshScope
@Controller
@Api(value = "文件在线预览", tags = "文件在线预览")
public class PreviewController {
    @Autowired
    private FileService fileService;
    //网关上下文地址
    @Value("${sei.gateway.context-path:/api-gateway}")
    private String gatewayPath;

    @GetMapping(value = "/pdf2Img/{docId}")
    @ApiOperation("在线预览")
    public String pdf2Img(@PathVariable("docId") String docId,
                          Model model, HttpServletRequest request) {
        String contextPath = request.getContextPath();
        if (StringUtils.isNotBlank(contextPath) && !StringUtils.startsWith(contextPath, "/")) {
            contextPath = "/" + contextPath;
        }
        model.addAttribute("baseUrl", gatewayPath.concat(contextPath));
        model.addAttribute("docId", docId);

        String view;
        DocumentResponse document = fileService.getDocument(docId);
        if (Objects.isNull(document) || StringUtils.isBlank(document.getFileName())) {
            view = "preview/notfound.html";
            model.addAttribute("docId", docId);
            return view;
        }
        String fileName = document.getFileName();
        switch (document.getDocumentType()) {
            case Word:
            case Powerpoint:
                // 文件预览转换
                ResultData<DocumentResponse> resultData = PreviewServiceFactory.getPreviewDocument(document);
                if (resultData.failed()) {
                    LogUtil.error("file convert error");
                    view = "preview/notfound.html";
                    model.addAttribute("docId", docId);
                    return view;
                }
                document = resultData.getData();
            case Pdf:
                PDDocument doc = null;
                try {
                    File file = new File(FileConstants.FILE_PATH + docId + ".pdf");
                    if (!file.exists()) {
                        FileUtils.writeByteArrayToFile(file, document.getData());
                    }
                    FileConstants.add(file);
                    // 解码PDF中的条码信息.实质是将pdf转为图片后再解码
                    doc = PDDocument.load(file);
                    model.addAttribute("pageCount", doc.getNumberOfPages() - 1);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (doc != null) {
                        try {
                            doc.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                view = "preview/officePicture.html";
                fileName = fileName.substring(0, fileName.lastIndexOf(".")).concat(".pdf");
                break;
            default:
                // 不支持
                view = "preview/nosupport.html";
                model.addAttribute("fileType", document.getDocumentType().name());
        }

        model.addAttribute("fileName", FileUtils.getFileName(fileName));
        return view;
    }

    /**
     * pdf文件转换成jpg图片集
     *
     * @return 图片访问集合
     */
    @GetMapping(value = "/pdfPage/{docId}/{page}")
    public ResponseEntity<byte[]> pdfPage(@PathVariable("docId") String docId, @PathVariable("page") Integer page,
                                          Model model, HttpServletRequest request, HttpServletResponse response) {
        //判断文件类型
        response.setContentType("image/jpeg");

        String contextPath = request.getContextPath();
        if (StringUtils.isNotBlank(contextPath) && !StringUtils.startsWith(contextPath, "/")) {
            contextPath = "/" + contextPath;
        }
        model.addAttribute("baseUrl", gatewayPath.concat(contextPath));
        model.addAttribute("docId", docId);

        PDDocument pdfDoc = null;
        // 解码PDF中的条码信息.实质是将pdf转为图片后再解码
        try {
            File file = new File(FileConstants.FILE_PATH + docId + ".pdf");
            if (!file.exists()) {
                DocumentResponse document = fileService.getDocument(docId);
                if (Objects.isNull(document) || StringUtils.isBlank(document.getFileName())) {
                    model.addAttribute("docId", docId);
                    return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
                }
                // 文件预览转换
                ResultData<DocumentResponse> resultData = PreviewServiceFactory.getPreviewDocument(document);
                if (resultData.failed()) {
                    return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
                }
                document = resultData.getData();

                FileUtils.writeByteArrayToFile(file, document.getData());
            }
            FileConstants.add(file);
            pdfDoc = PDDocument.load(file);
            int pageCount = pdfDoc.getNumberOfPages();
            model.addAttribute("pageCount", -1);
            if (pageCount < page) {
                page = pageCount - 1;
            }

            PDFRenderer pdfRenderer = new PDFRenderer(pdfDoc);
            BufferedImage image = pdfRenderer.renderImageWithDPI(page, 2*72, ImageType.RGB);
            OutputStream outputStream = response.getOutputStream();
            ImageIOUtil.writeImage(image, "jpg", outputStream, 2*72);

            return new ResponseEntity<>(new byte[0], HttpStatus.OK);
        } catch (Exception e) {
            LogUtil.error("readFile error: " + e.getMessage(), e);
        } finally {
            if (pdfDoc != null) {
                try {
                    pdfDoc.close();
                } catch (Exception ignored) {
                }
            }
        }
        return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
    }


    @GetMapping(value = "/preview")
    @ApiOperation("在线预览")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "docId", value = "附件id", required = true, paramType = "query"),
            @ApiImplicitParam(name = "markText", value = "水印内容", paramType = "query", example = "SEI6.0")
    })
    public String preview(@RequestParam("docId") String docId,
                          @RequestParam(name = "previewType", required = false) String previewType,
                          @RequestParam(name = "markText", required = false) String markText,
                          Model model, HttpServletRequest request) {
        String contextPath = request.getContextPath();
        if (StringUtils.isNotBlank(contextPath) && !StringUtils.startsWith(contextPath, "/")) {
            contextPath = "/" + contextPath;
        }
        model.addAttribute("baseUrl", gatewayPath.concat(contextPath));

        model.addAttribute("docId", docId);
        if (StringUtils.isNotBlank(markText)) {
            model.addAttribute("markText", markText);
        }

        String view;
        DocumentResponse document = fileService.getDocumentInfo(docId);
        if (Objects.isNull(document) || StringUtils.isBlank(document.getFileName())) {
            view = "preview/notfound.html";
            model.addAttribute("docId", docId);
            return view;
        }
        String fileName = document.getFileName();
        switch (document.getDocumentType()) {
            case Pdf:
            case Word:
            case Powerpoint:
                if (StringUtils.equalsIgnoreCase("image", previewType)) {
                    view = this.pdf2Img(docId, model, request);
                } else {
                    view = "preview/pdf.html";
                    fileName = fileName.substring(0, fileName.lastIndexOf(".")).concat(".pdf");
                }
                break;
            case Excel:
                view = "preview/html.html";
                break;
            case OFD:
                view = "preview/ofd.html";
                fileName = fileName.substring(0, fileName.lastIndexOf(".")).concat(".ofd");
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
            case Other:
            default:
                // 不支持
                view = "preview/nosupport.html";
                model.addAttribute("fileType", document.getDocumentType().name());
        }

        model.addAttribute("fileName", FileUtils.getFileName(fileName));
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
