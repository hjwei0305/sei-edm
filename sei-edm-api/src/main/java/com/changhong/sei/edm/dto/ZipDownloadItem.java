package com.changhong.sei.edm.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 实现功能：文件下载明细
 *
 * @author xiaogang.su
 * @version 1.0.00  2022年3月7日 11:11
 */
@ApiModel(description = "文件下载明细")
public class ZipDownloadItem implements Serializable {

    private static final long serialVersionUID = 4486783982051571672L;

    /**
     * 是否是文件夹
     */
    @NotNull
    @ApiModelProperty(name = "是否是文件夹")
    private Boolean isDirectory;
    /**
     * 文件名称
     */
    @ApiModelProperty(name = "文件名称")
    private String fileName;
    /**
     * EDM文档ID
     */
    @ApiModelProperty(name = "EDM文档ID")
    private String docId;
    /**
     * 文件夹下级文件列表
     */
    @ApiModelProperty(name = "文件夹下级文件列表",notes = "是文件夹时有效")
    private List<ZipDownloadItem> subFiles;

    public Boolean getDirectory() {
        return isDirectory;
    }

    public void setDirectory(Boolean directory) {
        isDirectory = directory;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public List<ZipDownloadItem> getSubFiles() {
        return subFiles;
    }

    public void setSubFiles(List<ZipDownloadItem> subFiles) {
        this.subFiles = subFiles;
    }
}
