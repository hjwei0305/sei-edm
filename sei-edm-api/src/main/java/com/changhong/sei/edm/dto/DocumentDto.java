package com.changhong.sei.edm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实现功能：文档(包含信息和数据)
 */
@ApiModel(description = "文档信息")
public class DocumentDto implements Serializable {
    private static final long serialVersionUID = -7003748738807976561L;
    // 文档id
    @ApiModelProperty(notes = "文档id")
    private String docId;
    /**
     * 文件MD5
     */
    @ApiModelProperty(notes = "文件MD5")
    private String fileMd5;
    /**
     * 文件名（包括后缀）
     */
    @ApiModelProperty(notes = "文件名（包括后缀）")
    private String fileName;
    /**
     * 来源应用系统
     */
    @ApiModelProperty(notes = "来源应用系统")
    private String system;
    /**
     * 上传人
     */
    @ApiModelProperty(notes = "上传人")
    private String uploadUser;
    /**
     * 上传时间
     */
    @ApiModelProperty(notes = "上传时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedTime;
    /**
     * 文件大小
     */
    @ApiModelProperty(notes = "文件大小")
    private long size;
    /**
     * 文件类型
     */
    @ApiModelProperty(notes = "文件类型")
    private DocumentType documentType;
    /**
     * 文件数据base64编码
     */
    @ApiModelProperty(notes = "文件数据base64编码")
    private String base64Data;
    /**
     * 文档数据
     */
    @JsonIgnore
    private byte[] data;
    /**
     * 水印
     */
    @JsonIgnore
    private String markText;

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getUploadUser() {
        return uploadUser;
    }

    public void setUploadUser(String uploadUser) {
        this.uploadUser = uploadUser;
    }

    public LocalDateTime getUploadedTime() {
        return uploadedTime;
    }

    public void setUploadedTime(LocalDateTime uploadedTime) {
        this.uploadedTime = uploadedTime;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public void setBase64Data(String base64Data) {
        this.base64Data = base64Data;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getMarkText() {
        return markText;
    }

    public void setMarkText(String markText) {
        this.markText = markText;
    }
}
