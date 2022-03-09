package com.changhong.sei.edm.file.controller;

import com.changhong.sei.core.context.ContextUtil;
import com.changhong.sei.core.context.SessionUser;
import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.common.util.DocumentTypeUtil;
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
import org.apache.commons.io.IOUtils;
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
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    @ApiImplicitParam(name = "fileMd5", value = "来源系统", required = true)
    @ResponseBody
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
    @ApiImplicitParams({
            @ApiImplicitParam(name = "chunkNumber", value = "当前文件块，从1开始", required = true),
            @ApiImplicitParam(name = "currentChunkSize", value = "当前分块大小", required = true),
            @ApiImplicitParam(name = "chunkSize", value = "分块大小", required = true),
            @ApiImplicitParam(name = "totalSize", value = "总大小", required = true),
            @ApiImplicitParam(name = "totalChunks", value = "总块数", required = true),
            @ApiImplicitParam(name = "fileMd5", value = "原整体文件MD5", required = true),
            @ApiImplicitParam(name = "file", value = "文件", required = true)
    })
    @ResponseBody
    @RequestMapping(path = "/uploadChunk", method = RequestMethod.POST)
    public ResultData<UploadResponse> uploadChunk(@RequestParam("file") MultipartFile file,
                                                  @RequestParam(value = "chunkNumber") Integer chunkNumber,
                                                  @RequestParam(value = "currentChunkSize") Long currentChunkSize,
                                                  @RequestParam(value = "chunkSize") Long chunkSize,
                                                  @RequestParam(value = "totalSize") Long totalSize,
                                                  @RequestParam(value = "totalChunks") Integer totalChunks,
                                                  @RequestParam(value = "fileMd5") String fileMd5) {
        LogUtil.debug("file originName: {}, chunkNumber: {}", file.getOriginalFilename(), chunkNumber);
        try {
            ResultData<UploadResponse> resultData = uploadFile(file, "SEI", "");
            if (resultData.successful()) {
                UploadResponse response = resultData.getData();
                LogUtil.debug("文件 {} 写入成功, docId:{}", response.getFileName(), response.getDocId());

                FileChunk fileChunk = new FileChunk();
                fileChunk.setDocId(response.getDocId());
                fileChunk.setChunkNumber(chunkNumber);
                fileChunk.setCurrentChunkSize(currentChunkSize);
                fileChunk.setChunkSize(chunkSize);
                fileChunk.setTotalSize(totalSize);
                fileChunk.setTotalChunks(totalChunks);
                fileChunk.setFileMd5(fileMd5);

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
            @ApiImplicitParam(name = "fileName", value = "文件名", required = true),
            @ApiImplicitParam(name = "fileMd5", value = "原整体文件MD5", required = true)
    })
    @ResponseBody
    @RequestMapping(path = "/mergeFile", method = RequestMethod.POST)
    public ResultData<UploadResponse> mergeFile(@RequestParam(name = "fileMd5") String fileMd5, @RequestParam(name = "fileName") String fileName) {
//        return fileService.mergeFile(fileMd5, fileName);
        ResultData<String> resultData = documentService.mergeFile(fileMd5, fileName);
        if (resultData.successful()) {
            UploadResponse response = new UploadResponse();
            response.setDocId(resultData.getData());
            response.setFileName(fileName);
            response.setDocumentType(DocumentTypeUtil.getDocumentType(fileName));
            return ResultData.success(response);
        }
        return ResultData.fail(resultData.getMessage());
    }

    @ApiOperation("URL文件上传或识别")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "sys", value = "来源系统"),
            @ApiImplicitParam(name = "uploadUser", value = "上传人"),
            @ApiImplicitParam(name = "ocr", dataTypeClass = OcrType.class, value = "ocr识别类型: None, Barcode, InvoiceQr, Qr "),
            @ApiImplicitParam(name = "fileUrl", value = "文件URL", required = true),
            @ApiImplicitParam(name = "fileName", value = "文件名", required = true)
    })
    @PostMapping(value = "/uploadByUrl")
    @ResponseBody
    public ResultData<UploadResponse> uploadByUrl(
            @RequestParam("fileUrl") String fileUrl,
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "uploadUser", required = false) String uploadUser,
            @RequestParam(value = "ocr", required = false) String ocr,
            @RequestParam(value = "sys", required = false) String sys) throws IOException {
        if (StringUtils.isBlank(sys)) {
            sys = ContextUtil.getAppCode();
        }

        byte[] fileBytes = null;
        InputStream inputStream = null;
        DocumentDto dto = new DocumentDto();
        try {
            URL url = new URL(fileUrl);
            inputStream = new BufferedInputStream(url.openStream());
            fileBytes = IOUtils.toByteArray(inputStream);
            dto.setData(fileBytes);
            // 计算文件MD5
            dto.setFileMd5(MD5Utils.md5(fileUrl));
            dto.setFileName(fileName);
            dto.setSystem(sys);
            if (StringUtils.isBlank(uploadUser)) {
                SessionUser user = ContextUtil.getSessionUser();
                uploadUser = user.getAccount();
            }
            dto.setUploadUser(uploadUser);
        } catch (Exception e) {
            LogUtil.error("通过URL[" + fileUrl + "]获取文件异常", e);
            return ResultData.fail("通过URL[" + fileUrl + "]获取文件异常");
        } finally {
            if (Objects.nonNull(inputStream)) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }

        ResultData<UploadResponse> resultData = fileService.uploadDocument(dto);
        ;
        if (resultData.successful() && StringUtils.isNotBlank(ocr)) {
            UploadResponse uploadResponse = resultData.getData();
            OcrType ocrType = EnumUtils.getEnum(OcrType.class, ocr);
            if (Objects.nonNull(uploadResponse) &&
                    Objects.nonNull(ocrType) && OcrType.None != ocrType) {
                // 字符识别
                ResultData<String> readerResult = characterReaderService.read(uploadResponse.getDocumentType(), ocrType, fileBytes);
                if (readerResult.successful()) {
                    // 设置识别的结果
                    uploadResponse.setOcrData(readerResult.getData());
                }
            }
        }
        return resultData;
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

    @ApiOperation("按附件id清理(该接口无效).默认将无业务关联的文档进行定时清理,因此若要删除只需解除业务关联绑定即可.")
    @ApiImplicitParam(name = "docIds", value = "附件id", required = true)
    @PostMapping(value = "/remove")
    @ResponseBody
    public ResultData<String> remove(@RequestParam(value = "docIds") String docIds) {
//        String[] docIdArr = StringUtils.split(docIds, ",");
//        Set<String> docIdSet = new HashSet<>();
//        for (String docId : docIdArr) {
//            docIdSet.add(docId.trim());
//        }
//        return fileService.removeByDocIds(docIdSet);
        return ResultData.success("默认将无业务关联的文档进行定时清理,因此若要删除只需解除业务关联绑定即可.");
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
            DocumentResponse document = fileService.getDocumentInfo(docId);
            if (Objects.nonNull(document) && StringUtils.isNotBlank(document.getFileName())) {
                return singleDownload(document, Boolean.TRUE, width, height, request, response);
            } else {
                LogUtil.error("file is not found");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }
    }

    // http://localhost:8080/file/download?docIds=BEFD5E57FBF011EA9A0E0242C0A84610&fileName=%E6%B5%8B%E8%AF%951.zip
    @ApiOperation("文件下载 docIds和entityId二选一. 当docIds存在多个时用post")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "docIds", value = "附件id"),
            @ApiImplicitParam(name = "entityId", value = "业务实体id"),
            @ApiImplicitParam(name = "fileName", value = "下载文件名")
    })
    @RequestMapping(value = "/download", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> download(@RequestParam(value = "docIds", required = false) String docIds,
                                           @RequestParam(value = "entityId", required = false) String entityId,
                                           @RequestParam(value = "fileName", required = false) String fileName,
                                           @RequestBody(required = false) DownloadRequest downloadRequest,
                                           HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {
        Set<String> docIdSet = null;
        if (Objects.nonNull(downloadRequest)) {
            fileName = downloadRequest.getFileName();
            docIdSet = downloadRequest.getDocIds();
        }

        if (StringUtils.isBlank(entityId)) {
            if (StringUtils.isBlank(docIds)) {
                if (CollectionUtils.isEmpty(docIdSet)) {
                    LogUtil.warn("下载参数错误.");
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                } else {
                    if (docIdSet.size() == 1) {
                        Optional<String> optional = docIdSet.stream().filter(StringUtils::isNotBlank).findFirst();
                        if (optional.isPresent()) {
                            // 单文件下载
                            DocumentResponse document = fileService.getDocumentInfo(optional.get());
                            if (Objects.nonNull(document) && StringUtils.isNotBlank(document.getFileName())) {
                                return singleDownload(document, Boolean.FALSE, 0, 0, request, response);
                            }
                        }
                    } else {
                        List<Document> documents = documentService.getDocs(docIdSet);
                        if (Objects.nonNull(documents)) {
                            if (documents.size() == 1) {
                                DocumentResponse documentResponse = new ModelMapper().map(documents.get(0), DocumentResponse.class);
                                return singleDownload(documentResponse, Boolean.FALSE, 0, 0, request, response);
                            } else {
                                // 多文件下载
                                return multipleDownload(fileName, documents, request, response);
                            }
                        }
                    }
                }
            } else {
                String[] docIdArr = StringUtils.split(docIds, ",");
                if (docIdArr.length == 1) {
                    // 单文件下载
                    DocumentResponse document = fileService.getDocumentInfo(docIdArr[0].trim());
                    if (Objects.nonNull(document) && StringUtils.isNotBlank(document.getFileName())) {
                        return singleDownload(document, Boolean.FALSE, 0, 0, request, response);
                    }
                } else {
                    docIdSet = new HashSet<>();
                    Collections.addAll(docIdSet, docIdArr);
                    List<Document> documents = documentService.getDocs(docIdSet);
                    if (Objects.nonNull(documents)) {
                        if (documents.size() == 1) {
                            DocumentResponse documentResponse = new ModelMapper().map(documents.get(0), DocumentResponse.class);
                            return singleDownload(documentResponse, Boolean.FALSE, 0, 0, request, response);
                        } else {
                            // 多文件下载
                            return multipleDownload(fileName, documents, request, response);
                        }
                    }
                }
            }
        } else {
            List<Document> documents = documentService.getDocumentsByEntityId(entityId);
            if (Objects.nonNull(documents)) {
                if (documents.size() == 1) {
                    DocumentResponse documentResponse = new ModelMapper().map(documents.get(0), DocumentResponse.class);
                    return singleDownload(documentResponse, Boolean.FALSE, 0, 0, request, response);
                } else {
                    // 多文件下载
                    return multipleDownload(fileName, documents, request, response);
                }
            }
        }

        LogUtil.error("file is not found");
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * 多文件打包压缩下载（支持自定义文件夹）
     *
     * @param zipDownloadRequest 文件下载参数
     * @return 二进制数据
     * @throws Exception
     */
    @ApiOperation("多文件打包压缩下载(支持自定义文件夹)")
    @RequestMapping(value = "/zipDownload", method = RequestMethod.POST)
    public ResponseEntity<byte[]> zipDownload(@RequestBody @Valid ZipDownloadRequest zipDownloadRequest, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String zipFileName = zipDownloadRequest.getFileName();
        if (StringUtils.isBlank(zipFileName)) {
            zipFileName = IdGenerator.uuid2() + ".zip";
        }
        List<ZipDownloadItem> items = zipDownloadRequest.getItems();
        //设置下载文件名
        this.setDownloadFileName(zipFileName, request, response);
        //判断是否重名，同一文件夹下不允许重名，否则会出现下载到一半失败，导致内容缺少
        //本应该在添加文件时捕获异常，但是底层抛出的异常都是统一的ZipException
        Set<String> names = new HashSet<>();
        OutputStream outputStream = response.getOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            //遍历文件
            this.addItemsToZip(zos, "", items, names);
            // 完成编写ZIP输出流的内容而不关闭底层流
            zos.finish();
            outputStream.flush();
        } catch (Exception e) {
            LogUtil.error("打包文件异常", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * 迭代循环添加文档至压缩文件
     *
     * @param zos   压缩文件流
     * @param path  工作目录
     * @param items 文件列表
     * @throws IOException
     */
    private void addItemsToZip(ZipOutputStream zos, String path, List<ZipDownloadItem> items, Set<String> names) throws IOException {
        if (CollectionUtils.isNotEmpty(items)) {
            ZipEntry zipEntry;
            for (ZipDownloadItem item : items) {
                if (item.getDirectory()) {
                    //路径前缀
                    String wordPath = path + item.getFileName() + "/";
                    //添加文件夹 迭代
                    addItemsToZip(zos, wordPath, item.getSubFiles(), names);
                } else {
                    DocumentResponse documentResponse = fileService.getDocumentInfo(item.getDocId());
                    if (Objects.nonNull(documentResponse)) {
                        String fileName = path + item.getFileName();
                        if (StringUtils.isBlank(item.getFileName())) {
                            zipEntry = new ZipEntry(path + documentResponse.getFileName());
                        }
                        String fileNameWithoutExtension = FileUtils.getWithoutExtension(fileName);
                        String extension = FileUtils.getExtension(fileName);
                        //判断是否重名
                        int index = 2;
                        while (!names.add(fileName)) {
                            //在原始的文件后面拼上序号
                            fileName = fileNameWithoutExtension + index + extension;
                            index++;
                        }
                        zipEntry = new ZipEntry(fileName);
                        // 开始编写新的ZIP文件条目并将流定位到条目数据的开头
                        zos.putNextEntry(zipEntry);
                        fileService.getDocumentOutputStream(item.getDocId(), documentResponse.getHasChunk(), zos);
                        // 关闭当前的ZIP条目并定位写入下一个条目的流
                        zos.closeEntry();
                    }
                }
            }
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
     * @param document    文档信息
     * @param isThumbnail 缩略图
     */
    private ResponseEntity<byte[]> singleDownload(DocumentResponse document, boolean isThumbnail, int width, int height,
                                                  HttpServletRequest request, HttpServletResponse response) {
        if (Objects.isNull(document)) {
            LogUtil.error("file is not found");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (isThumbnail) {
            // 设置下载文件名
            if (DocumentType.Image.equals(document.getDocumentType())) {
                setDownloadFileName(document.getFileName(), request, response);
            } else {
                setDownloadFileName(document.getFileName() + "_Thumbnail.png", request, response);
            }

            document = fileService.getThumbnail(document.getDocId(), width, height);
            byte[] buffer = new byte[2048];
            try (InputStream is = new ByteArrayInputStream(document.getData()); BufferedInputStream bis = new BufferedInputStream(is)) {
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
            }
        } else {
            // 设置下载文件名
            setDownloadFileName(document.getFileName(), request, response);
            try {
                OutputStream os = response.getOutputStream();
                fileService.getDocumentOutputStream(document.getDocId(), document.getHasChunk(), os);
                os.flush();
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (IOException e) {
                LogUtil.error("Download error: " + e.getMessage(), e);
            }
        }
        return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
    }

    private ResponseEntity<byte[]> multipleDownload(String zipFileName, List<Document> documents, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (CollectionUtils.isNotEmpty(documents)) {
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
                fileName = new String(fileName.getBytes(StandardCharsets.UTF_8), "ISO8859-1");
            }
            /*
                ERR_RESPONSE_HEADERS_MULTIPLE_CONTENT_DISPOSITION：下载报错，浏览器network error错误
                https://blog.csdn.net/qq_37837134/article/details/84644097
                浏览器中filename中包含特殊的标点符号，浏览器误认为是HTTP响应拆分攻击。所以在filename中加引号包裹，以告诉浏览器是一个文件名。
             */
            response.addHeader("Content-Disposition", "attachment;fileName=\"" + fileName + "\"");
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
            ZipEntry zipEntry;

            for (Document doc : documents) {
                try {
//                    document = fileService.getDocument(doc.getDocId());
//                    zipEntry = new ZipEntry(doc.getFileName());
//                    // 开始编写新的ZIP文件条目并将流定位到条目数据的开头
//                    zip.putNextEntry(zipEntry);
//                    byte[] data = document.getData();
//                    zip.write(data, 0, data.length);

                    zipEntry = new ZipEntry(doc.getFileName());
                    // 开始编写新的ZIP文件条目并将流定位到条目数据的开头
                    zip.putNextEntry(zipEntry);
                    fileService.getDocumentOutputStream(doc.getDocId(), doc.getHasChunk(), zip);

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
