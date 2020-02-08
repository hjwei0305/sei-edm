package com.changhong.sei.edm.file.controller;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.file.service.FileService;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;


@Controller
@RequestMapping(value = "/file")
@Api(value = "文件上传下载", tags = "文件上传下载")
public class FileController {

    @Autowired
    private FileService fileService;
//    @Autowired
//    private HttpServletRequest request;
//
//    @Autowired
//    private HttpServletResponse response;

    @ApiOperation("文件上传")
    @ApiImplicitParam(name = "file", value = "文件", required = true)
    @PostMapping(value = "/upload")
    @ResponseBody
    public ResultData<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        ResultData<String> resultData = fileService.uploadDocument(file.getOriginalFilename(), file.getBytes());
        return resultData;
    }

    @ApiOperation("文件下载 docIds和entityId二选一")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "docIds", value = "附件id", paramType = "query"),
            @ApiImplicitParam(name = "entityId", value = "业务实体id", paramType = "query"),
            @ApiImplicitParam(name = "isThumbnail", value = "缩略图", paramType = "query", dataType = "boolean", defaultValue = "false")
    })
    @GetMapping(value = "/download")
    public ResponseEntity<byte[]> download(@RequestParam(value = "docIds", required = false) String docIds,
                                           @RequestParam(value = "entityId", required = false) String entityId,
                                           @RequestParam(value = "isThumbnail", defaultValue = "false") boolean isThumbnail,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        if (StringUtils.isBlank(entityId)) {
            if (Objects.isNull(docIds)) {
                LogUtil.warn("下载参数错误.");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            } else {
                String[] docIdArr = StringUtils.split(docIds, ",");
                if (docIdArr.length == 1) {
                    // 单文件下载
                    return singleDownload(docIdArr[0].trim(), isThumbnail, request, response);
                } else {

                    // 多文件下载
                    return multipleDownload(request, response);
                }
            }
        } else {
            // 多文件下载
            return multipleDownload(request, response);
        }
    }

    /**
     * 单文件下载
     *
     * @param docId       docId
     * @param isThumbnail 缩略图
     */
    private ResponseEntity<byte[]> singleDownload(String docId, boolean isThumbnail,
                                                  HttpServletRequest request, HttpServletResponse response) {
        DocumentResponse document = fileService.getDocument(docId, isThumbnail);
        if (Objects.isNull(document)) {
            LogUtil.error("file is not found");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // 设置下载文件名
        setDownloadFileName(document.getFileName(), request, response);
        try {
            byte[] bytes = document.getData();
            response.getOutputStream().write(bytes);
            return new ResponseEntity<>(bytes, HttpStatus.OK);
        } catch (IOException e) {
            LogUtil.error("Download error: " + e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
    }

    private ResponseEntity<byte[]> multipleDownload(HttpServletRequest request, HttpServletResponse response) {


        return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
    }

    /**
     * 设置下载文件名
     */
    private void setDownloadFileName(String fileName, HttpServletRequest request, HttpServletResponse response) {
        //清空response
        response.reset();
        // 设置强制下载不打开
        //response.setContentType("application/force-download");
        response.setContentType("application/octet-stream");
        // 设置文件名
        try {
            /*
             * IE，通过URLEncoder对filename进行UTF8编码
             * 其他的浏览器（firefox、chrome、safari、opera），则要通过字节转换成ISO8859-1
             */
            if (StringUtils.containsAny(request.getHeader("User-Agent").toLowerCase(), "msie", "edge")) {
                fileName = URLEncoder.encode(fileName, "UTF-8");
            } else {
                fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
            }
            response.addHeader("Content-Disposition", "attachment;fileName=" + fileName);
        } catch (UnsupportedEncodingException e) {
            LogUtil.error("文件名编码错误", e);
        }
    }
}
