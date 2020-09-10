package com.changhong.sei.edm.file.controller;

import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.context.SessionUser;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.dto.serach.SearchFilter;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.common.util.MD5Utils;
import com.changhong.sei.edm.dto.*;
import com.changhong.sei.edm.file.service.FileService;
import com.changhong.sei.edm.manager.entity.Document;
import com.changhong.sei.edm.manager.entity.FileChunk;
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
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
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

    @ApiOperation("检查分片")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "fileMd5", value = "来源系统")
    })
    @RequestMapping(path = "/checkChunk", method = RequestMethod.GET)
    public ResultData<FileChunkResponse> checkChunk(@RequestParam(name = "fileMd5") String fileMd5) {
        FileChunkResponse response = new FileChunkResponse();
        // 通过源文件的md5检查是否有存在的文件,存在返回docId
        Document document = documentService.getDocumentByMd5(fileMd5);
        if (Objects.nonNull(document)) {
            response.setDocId(document.getDocId());
            // 上传状态为完成
            response.setUploadState(FileChunkResponse.UploadEnum.completed);
            return ResultData.success(response);
        }

        // 当不存在.继续检查是否是续传(之前有分块未传完或合并),存在则返回已上传完成的文件块
        List<FileChunk> chunks = documentService.getFileChunk(fileMd5);
        if (CollectionUtils.isNotEmpty(chunks)) {
            FileChunkDto chunkDto = null;
            ModelMapper modelMapper = new ModelMapper();
            List<FileChunkDto> chunkDtos = new ArrayList<>();
            for (FileChunk chunk : chunks) {
                chunkDto = modelMapper.map(chunk, FileChunkDto.class);
                chunkDtos.add(chunkDto);
            }

            if (Objects.nonNull(chunkDto)) {
                response.setChunks(chunkDtos);
                // 上传状态为部分完成
                response.setUploadState(FileChunkResponse.UploadEnum.undone);
                response.setTotalChunks(chunkDto.getTotalChunks());
                response.setTotalSize(chunkDto.getTotalSize());
                response.setChunkSize(chunkDto.getChunkSize());

                return ResultData.success(response);
            }
        }

        // 不存在.返回上传状态UploadEnum.none
        response.setUploadState(FileChunkResponse.UploadEnum.none);
        return ResultData.success(response);
    }

    @ApiOperation("文件分片上传")
    @RequestMapping(path = "/uploadChunk", method = RequestMethod.POST)
    public ResultData<UploadResponse> uploadChunk(@RequestBody @Valid FileChunkRequest chunk) {
        MultipartFile file = chunk.getFile();
        LogUtil.debug("file originName: {}, chunkNumber: {}", file.getOriginalFilename(), chunk.getChunkNumber());
        try {
            ResultData<UploadResponse> resultData = uploadFile(file, "SEI", "");
            if (resultData.successful()) {
                UploadResponse response = resultData.getData();
                LogUtil.debug("文件 {} 写入成功, docId:{}", response.getFileName(), chunk.getDocId());

                FileChunk fileChunk = new ModelMapper().map(chunk, FileChunk.class);
                documentService.saveFileChunk(fileChunk, response.getDocId(), response.getFileName());

                return ResultData.success(response);
            } else {
                return ResultData.fail(resultData.getMessage());
            }
        } catch (IOException e) {
            LogUtil.error("上传异常", e);
            return ResultData.fail("上传异常:" + e.getMessage());
        }
    }

    @ApiOperation("合并文件分片")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "sys", value = "来源系统"),
            @ApiImplicitParam(name = "uploadUser", value = "上传人"),
            @ApiImplicitParam(name = "ocr", dataTypeClass = OcrType.class, value = "ocr识别类型: None, Barcode, InvoiceQr, Qr "),
            @ApiImplicitParam(name = "file", value = "文件", required = true)
    })
    @RequestMapping(path = "/mergeFile", method = RequestMethod.POST)
    public ResultData<UploadResponse> mergeFile(@RequestParam(name = "fileMd5") String fileMd5, @RequestParam(name = "fileName") String fileName) {
        return fileService.mergeFile(fileMd5, fileName);
    }

    @ApiOperation("单文件上传或识别")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "sys", value = "来源系统"),
            @ApiImplicitParam(name = "uploadUser", value = "上传人"),
            @ApiImplicitParam(name = "ocr", dataTypeClass = OcrType.class, value = "ocr识别类型: None, Barcode, InvoiceQr, Qr "),
            @ApiImplicitParam(name = "file", value = "文件", required = true)
    })
    @PostMapping(value = "/upload")
    @ResponseBody
    public ResultData<UploadResponse> upload(//@RequestParam("file") MultipartFile[] files,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "uploadUser", required = false) String uploadUser,
                                             @RequestParam(value = "ocr", required = false) String ocr,
                                             @RequestParam(value = "sys", required = false) String sys) throws IOException {
        if (StringUtils.isBlank(sys)) {
            sys = ContextUtil.getAppCode();
        }
        ResultData<UploadResponse> resultData = uploadFile(file, sys, uploadUser);
        if (resultData.successful() && StringUtils.isNotBlank(ocr)) {
            UploadResponse uploadResponse = resultData.getData();
            OcrType ocrType = EnumUtils.getEnum(OcrType.class, ocr);
            if (Objects.nonNull(uploadResponse) &&
                    Objects.nonNull(ocrType) && OcrType.None != ocrType) {
                // 字符识别
                ResultData<String> readerResult = characterReaderService.read(uploadResponse.getDocumentType(), ocrType, file.getBytes());
                if (readerResult.successful()) {
                    // 设置识别的结果
                    uploadResponse.setOcrData(readerResult.getData());
                }
            }
        }
        return resultData;
    }

    @ApiOperation("多文件批量上传")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "sys", value = "来源系统"),
            @ApiImplicitParam(name = "uploadUser", value = "上传人"),
            @ApiImplicitParam(name = "ocr", dataTypeClass = OcrType.class, value = "ocr识别类型: None, Barcode, InvoiceQr, Qr "),
            @ApiImplicitParam(name = "file", value = "文件", required = true)
    })
    @PostMapping(value = "/batchUpload")
    @ResponseBody
    public ResultData<List<UploadResponse>> batchUpload(@RequestParam("file") MultipartFile[] files,
                                                        @RequestParam(value = "ocr", required = false) String ocr,
                                                        @RequestParam(value = "uploadUser", required = false) String uploadUser,
                                                        @RequestParam(value = "sys", required = false) String sys) throws IOException {
        if (StringUtils.isBlank(sys)) {
            sys = ContextUtil.getAppCode();
        }
        if (StringUtils.isBlank(uploadUser)) {
            SessionUser user = ContextUtil.getSessionUser();
            uploadUser = user.getAccount();
        }

        DocumentDto dto;
        UploadResponse uploadResponse;
        List<UploadResponse> uploadResponses = new ArrayList<>();
        for (MultipartFile file : files) {
            dto = new DocumentDto();
            dto.setData(file.getBytes());
            dto.setFileName(file.getOriginalFilename());
            dto.setSystem(sys);

            dto.setUploadUser(uploadUser);

            // 文件上传
            ResultData<UploadResponse> resultData = fileService.uploadDocument(dto);
            if (resultData.successful()) {
                uploadResponse = resultData.getData();
                if (StringUtils.isNotBlank(ocr)) {
                    OcrType ocrType = EnumUtils.getEnum(OcrType.class, ocr);
                    if (Objects.nonNull(uploadResponse) &&
                            Objects.nonNull(ocrType) && OcrType.None != ocrType) {
                        // 字符识别
                        ResultData<String> readerResult = characterReaderService.read(uploadResponse.getDocumentType(), ocrType, file.getBytes());
                        if (readerResult.successful()) {
                            // 设置识别的结果
                            uploadResponse.setOcrData(readerResult.getData());
                        }
                    }
                }
                uploadResponses.add(uploadResponse);
            }
        }
        return ResultData.success(uploadResponses);
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

    private ResultData<UploadResponse> uploadFile(MultipartFile file, String sys, String uploadUser) throws IOException {
        DocumentDto dto = new DocumentDto();
        dto.setData(file.getBytes());
        // 计算文件MD5
        dto.setFileMd5(MD5Utils.md5Stream(file.getInputStream()));
        dto.setFileName(file.getOriginalFilename());
        dto.setSystem(sys);
        if (StringUtils.isBlank(uploadUser)) {
            SessionUser user = ContextUtil.getSessionUser();
            uploadUser = user.getAccount();
        }
        dto.setUploadUser(uploadUser);

        // 文件上传
        return fileService.uploadDocument(dto);
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

        byte[] buffer = new byte[2048];
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            byte[] bytes = document.getData();
//            response.getOutputStream().write(bytes);
            is = new ByteArrayInputStream(bytes);
            bis = new BufferedInputStream(is);
            OutputStream os = response.getOutputStream();
            int i = bis.read(buffer);
            while (i != -1) {
                os.write(buffer, 0, i);
                i = bis.read(buffer);
            }
            os.flush();
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException e) {
            LogUtil.error("Download error: " + e.getMessage(), e);
        } finally {
            if (Objects.nonNull(bis)) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (Objects.nonNull(is)) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
        }
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
