package com.changhong.sei.edm.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * 实现功能：文档(包含信息和数据)
 */
public class DocumentResponse extends DocumentDto implements Serializable {
    private static final long serialVersionUID = -7003748738807976561L;

    /**
     * 文件描述
     */
    private String description;
    /**
     * 来源应用系统
     */
    private String system;
    /**
     * 租户代码
     */
    private String tenantCode;
    /**
     * 上传时间
     */
    private Date uploadedTime;
    /**
     * 上传用户Id
     */
    private String uploadUserId;
    /**
     * 上传用户账号
     */
    private String uploadUserAccount;
    /**
     * 上传用户姓名
     */
    private String uploadUserName;

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getUploadedTime() {
        return uploadedTime;
    }

    public void setUploadedTime(Date uploadedTime) {
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

    /**
     * 获取文件大小（K或M）
     */
    public String getFileSize() {
        String fileSize;
        long length = getSize();
        if (length == 0) {
            return "0K";
        }
        if (length < 1024 * 1024) {
            long ksize = length / 1024;
            if (ksize == 0) {
                ksize = 1;
            }
            fileSize = String.format("%dK", ksize);
        } else {
            long msize = length / (1024 * 1024);
            fileSize = String.format("%dM", msize);
        }
        return fileSize;
    }
}
