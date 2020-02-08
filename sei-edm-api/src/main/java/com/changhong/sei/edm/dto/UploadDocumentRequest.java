package com.changhong.sei.edm.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-06 15:53
 */
public class UploadDocumentRequest implements Serializable {
    private static final long serialVersionUID = 401309802834000068L;
    /**
     * 文件名（包括后缀）
     */
    private String fileName;
    /**
     * 文件大小
     */
    private Long size;
    /**
     * 文件描述
     */
    private String description;
    /**
     * 来源应用系统
     */
    private String system;

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
}
