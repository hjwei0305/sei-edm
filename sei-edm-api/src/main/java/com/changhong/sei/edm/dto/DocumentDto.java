package com.changhong.sei.edm.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

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
     * 文件名（包括后缀）
     */
    @ApiModelProperty(notes = "文件名（包括后缀）")
    private String fileName;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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
