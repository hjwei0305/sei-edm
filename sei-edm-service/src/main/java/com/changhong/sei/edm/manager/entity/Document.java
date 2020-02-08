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
     * 文件描述
     */
    @Column(name = "description")
    private String description;
    /**
     * 来源应用系统
     */
    @Column(name = "system", length = 50)
    private String system;
    /**
     * 上传时间
     */
    @Column(name = "uploaded_time")
    private LocalDateTime uploadedTime;
    /**
     * 租户代码
     */
    @Column(name = "tenant_code", length = 50)
    private String tenantCode;
    /**
     * 上传用户Id
     */
    @Column(name = "upload_user_id", length = 36)
    private String uploadUserId;
    /**
     * 上传用户账号
     */
    @Column(name = "upload_user_account", length = 100)
    private String uploadUserAccount;
    /**
     * 上传用户姓名
     */
    @Column(name = "upload_user_name", length = 100)
    private String uploadUserName;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getUploadUserId() {
        return uploadUserId;
    }

    public void setUploadUserId(String uploadUserId) {
        this.uploadUserId = uploadUserId;
    }

    public String getUploadUserAccount() {
        return uploadUserAccount;
    }

    public void setUploadUserAccount(String uploadUserAccount) {
        this.uploadUserAccount = uploadUserAccount;
    }

    public String getUploadUserName() {
        return uploadUserName;
    }

    public void setUploadUserName(String uploadUserName) {
        this.uploadUserName = uploadUserName;
    }
}
