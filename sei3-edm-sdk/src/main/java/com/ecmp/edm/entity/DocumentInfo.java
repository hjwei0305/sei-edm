package com.ecmp.edm.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：文档信息
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017-07-10 15:20      王锦光(wangj)                新建
 * <p/>
 * *************************************************************************************************
 */
public class DocumentInfo {
    // 主键
    private String id;
    /**
     * 应用模块代码
     */
    private String appModule;
    /**
     * 文件名（包括后缀）
     */
    private String fileName;
    /**
     * 文件大小
     */
    private Long size;
    /**
     * 上传时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date uploadedTime;
    /**
     * 上传用户姓名
     */
    private String uploadUserName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppModule() {
        return appModule;
    }

    public void setAppModule(String appModule) {
        this.appModule = appModule;
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

    public Date getUploadedTime() {
        return uploadedTime;
    }

    public void setUploadedTime(Date uploadedTime) {
        this.uploadedTime = uploadedTime;
    }

    public String getUploadUserName() {
        return uploadUserName;
    }

    public void setUploadUserName(String uploadUserName) {
        this.uploadUserName = uploadUserName;
    }

}
