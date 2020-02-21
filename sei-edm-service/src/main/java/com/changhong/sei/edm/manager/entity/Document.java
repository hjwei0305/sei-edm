package com.changhong.sei.edm.manager.entity;

import com.changhong.sei.core.entity.BaseEntity;
import com.changhong.sei.edm.dto.DocumentType;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 实现功能：文档信息
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-03 15:44
 */
@Entity
@Table(name = "edm_document_info")
@DynamicInsert
@DynamicUpdate
public class Document extends BaseEntity {
    private static final long serialVersionUID = 1189243298194997374L;
    public static final String FIELD_DOC_ID = "docId";

    /**
     * docId
     * local: 当本地存储时,为真实文件名(不含路径)
     * mongo: 当mongo存储时,为mongo返回的id
     */
    @Column(name = "doc_id", length = 100, unique = true)
    private String docId;
    /**
     * 文件名（包括后缀,不含路径）
     */
    @Column(name = "file_name", length = 100)
    private String fileName;
    /**
     * 文件大小
     */
    @Column(name = "size")
    private Long size;
    /**
     * 文件类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 30)
    private DocumentType documentType;
    /**
     * 来源应用系统(应用服务名)
     */
    @Column(name = "system", length = 50)
    private String system;
    /**
     * 上传时间
     */
    @Column(name = "uploaded_time")
    private LocalDateTime uploadedTime;

    public Document() {
    }

    public Document(String fileName) {
        this.fileName = fileName;
    }

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

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public LocalDateTime getUploadedTime() {
        return uploadedTime;
    }

    public void setUploadedTime(LocalDateTime uploadedTime) {
        this.uploadedTime = uploadedTime;
    }
}
