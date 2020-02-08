package com.changhong.sei.edm.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * 实现功能：文档(包含信息和数据)
 */
public class DocumentDto implements Serializable {
    private static final long serialVersionUID = -7003748738807976561L;
    // 主键
    private String docId;
    /**
     * 文件名（包括后缀）
     */
    private String fileName;
    /**
     * 文件大小
     */
    private long size;
    /**
     * 文件类型
     */
    private DocumentType documentType;
    /**
     * 文档数据
     */
    @JsonIgnore
    private byte[] data;

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

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
