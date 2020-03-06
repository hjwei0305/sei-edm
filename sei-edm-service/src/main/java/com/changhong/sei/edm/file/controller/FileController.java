package com.changhong.sei.edm.file.controller;

import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.dto.serach.SearchFilter;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.dto.OcrType;
import com.changhong.sei.edm.dto.UploadResponse;
import com.changhong.sei.edm.file.service.FileService;
import com.changhong.sei.edm.manager.entity.Document;
import com.changhong.sei.edm.manager.service.DocumentService;
import com.changhong.sei.edm.ocr.service.CharacterReaderService;
import com.changhong.sei.util.EnumUtils;
import com.changhong.sei.util.IdGenerator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.CollectionUtils;
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
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping(value = "/file")
@Api(value = "文件上传下载", tags = "文件上传下载")
public class FileController {

    @Autowired
    private FileService fileService;
    @Autowired
    private CharacterReaderService characterReaderService;
    @Autowired
    private DocumentService documentService;

    @ApiOperation("文件上传")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "sys", value = "来源系统"),
            @ApiImplicitParam(name = "ocr", dataTypeClass = OcrType.class, value = "ocr识别类型: None, Barcode, InvoiceQr "),
            @ApiImplicitParam(name = "file", value = "文件", required = true)
    })
    @PostMapping(value = "/upload")
    @ResponseBody
    public ResultData<UploadResponse> upload(//@RequestParam("file") MultipartFile[] files,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "ocr", required = false) String ocr,
                                             @RequestParam(value = "sys", required = false) String sys) throws IOException {
        if (StringUtils.isBlank(sys)) {
            sys = ContextUtil.getAppCode();
        }
        UploadResponse uploadResponse;
//        for (MultipartFile file : files) {
        // 文件上传
        ResultData<UploadResponse> resultData = fileService.uploadDocument(file.getOriginalFilename(), sys, file.getBytes());
        if (resultData.successful() && StringUtils.isNotBlank(ocr)) {
            uploadResponse = resultData.getData();
            OcrType ocrType = EnumUtils.getEnum(OcrType.class, ocr);
            if (Objects.nonNull(ocrType) && OcrType.None != ocrType) {
                // 字符识别
                ResultData<String> readerResult = characterReaderService.read(ocrType, file.getBytes());
                if (readerResult.successful()) {
                    // 设置识别的结果
                    uploadResponse.setOcrData(readerResult.getData());
                }
            }
        }
        return resultData;
//            uploadResponse = resultData.getData();
//        }
//        return ResultData.success(uploadResponse);
    }

    @ApiOperation("按附件id清理")
    @ApiImplicitParam(name = "docIds", value = "附件id", required = true)
    @PostMapping(value = "/remove")
    @ResponseBody
    public ResultData<String> remove(@RequestParam(value = "docIds") String docIds) {
        String[] docIdArr = StringUtils.split(docIds, ",");
        Set<String> docIdSet = new HashSet<>();
        for (String docId : docIdArr) {
            docIdSet.add(docId.trim());
        }
        return fileService.removeByDocIds(docIdSet);
    }

    @ApiOperation("清理所有无效文档(删除无业务信息的文档)")
    @PostMapping(value = "/removeInvalid")
    @ResponseBody
    public ResultData<String> removeInvalid() {
        return fileService.removeInvalidDocuments();
    }

    @ApiOperation("获取缩略图")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "docId", value = "附件id", paramType = "query", required = true),
            @ApiImplicitParam(name = "width", value = "缩略图宽(默认:150)", paramType = "query"),
            @ApiImplicitParam(name = "height", value = "缩略图高(默认:100)", paramType = "query")
    })
    @GetMapping(value = "/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@RequestParam(value = "docId") String docId,
                                            @RequestParam(value = "width", required = false, defaultValue = "150") int width,
                                            @RequestParam(value = "height", required = false, defaultValue = "100") int height,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {

        if (StringUtils.isBlank(docId)) {
            LogUtil.warn("缩略图参数错误.");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } else {
            // 单文件下载
            return singleDownload(docId, Boolean.TRUE, width, height, request, response);
        }

    }

    @ApiOperation("文件下载 docIds和entityId二选一")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "docIds", value = "附件id", paramType = "query"),
            @ApiImplicitParam(name = "entityId", value = "业务实体id", paramType = "query"),
            @ApiImplicitParam(name = "fileName", value = "下载文件名", paramType = "query")
    })
    @GetMapping(value = "/download")
    public ResponseEntity<byte[]> download(@RequestParam(value = "docIds", required = false) String docIds,
                                           @RequestParam(value = "entityId", required = false) String entityId,
                                           HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {
        if (StringUtils.isBlank(entityId)) {
            if (StringUtils.isBlank(docIds)) {
                LogUtil.warn("下载参数错误.");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            } else {
                String[] docIdArr = StringUtils.split(docIds, ",");
                if (docIdArr.length == 1) {
                    // 单文件下载
                    return singleDownload(docIdArr[0].trim(), Boolean.FALSE, 0, 0, request, response);
                } else {
                    SearchFilter filter = new SearchFilter(Document.FIELD_DOC_ID, docIdArr, SearchFilter.Operator.IN);
                    List<Document> documents = documentService.findByFilter(filter);
                    // 多文件下载
                    return multipleDownload(documents, request, response);
                }
            }
        } else {
            List<Document> documents = documentService.getDocumentsByEntityId(entityId);
            // 多文件下载
            return multipleDownload(documents, request, response);
        }
    }

    /**
     * 单文件下载
     *
     * @param docId       docId
     * @param isThumbnail 缩略图
     */
    private ResponseEntity<byte[]> singleDownload(String docId, boolean isThumbnail, int width, int height,
                                                  HttpServletRequest request, HttpServletResponse response) {
        DocumentResponse document;
        if (isThumbnail) {
            document = fileService.getThumbnail(docId, width, height);
        } else {
            document = fileService.getDocument(docId);
        }
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

    private ResponseEntity<byte[]> multipleDownload(List<Document> documents, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (CollectionUtils.isNotEmpty(documents)) {
            String zipFileName = request.getParameter("fileName");
            if (StringUtils.isBlank(zipFileName)) {
                zipFileName = IdGenerator.uuid2() + ".zip";
            }
            // 设置下载文件名
            setDownloadFileName(zipFileName, request, response);

            // 压缩文件
            zipDocument(documents, response.getOutputStream());
        }
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
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
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

    /**
     * 打包压缩文件
     *
     * @param documents 文档信息
     */
    public void zipDocument(List<Document> documents, OutputStream outputStream) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }

        try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            DocumentResponse document;
            ZipEntry zipEntry;

            for (Document doc : documents) {
                try {
                    document = fileService.getDocument(doc.getDocId());
                    zipEntry = new ZipEntry(doc.getFileName());
                    // 开始编写新的ZIP文件条目并将流定位到条目数据的开头
                    zip.putNextEntry(zipEntry);
                    byte[] data = document.getData();
                    zip.write(data, 0, data.length);

                    // 关闭当前的ZIP条目并定位写入下一个条目的流
                    zip.closeEntry();
                } catch (Exception e) {
                    LogUtil.error("批量下载压缩异常", e);
                }
            }
            // 完成编写ZIP输出流的内容而不关闭底层流
            zip.finish();
            outputStream.flush();
        } catch (Exception e) {
            LogUtil.error("打包文件异常", e);
        }
    }
}
