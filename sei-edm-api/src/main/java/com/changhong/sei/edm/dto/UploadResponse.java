package com.changhong.sei.edm.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.StringJoiner;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-03-06 11:11
 */
@ApiModel(description = "文件上传响应")
public class UploadResponse implements Serializable {
    private static final long serialVersionUID = 8539065361214753205L;

    // 文档id
    @ApiModelProperty(notes = "文档id")
    private String docId;
    /**
     * 文件名（包括后缀）
     */
    @ApiModelProperty(notes = "文件名（包括后缀）")
    private String fileName;
    /**
     * 文件类型
     */
    @ApiModelProperty(notes = "文件类型")
    private DocumentType documentType;
    /**
     * 识别结果
     */
    @ApiModelProperty(notes = "识别结果")
    private String ocrData;

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

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getOcrData() {
        return ocrData;
    }

    public void setOcrData(String ocrData) {
        this.ocrData = ocrData;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UploadResponse.class.getSimpleName() + "[", "]")
                .add("docId='" + docId + "'")
                .add("fileName='" + fileName + "'")
                .toString();
    }
}
