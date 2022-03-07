package com.changhong.sei.edm.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 实现功能：带目录多文件打包压缩下载请求
 *
 * @author xiaogang.su
 * @version 1.0.00  2022年3月7日 11:11
 */
@ApiModel(description = "带目录多文件打包压缩下载请求")
public class ZipDownloadRequest implements Serializable {
    private static final long serialVersionUID = -6400814409626654118L;

    /**
     * 下载文件名称（包括后缀）
     */
    @ApiModelProperty(name = "下载文件名称", notes = "包括后缀")
    private String fileName;
    /**
     * 文件下载明细列表
     */
    @Size(min = 1)
    @Valid
    @ApiModelProperty(name = "文件下载明细列表")
    private List<ZipDownloadItem> items;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<ZipDownloadItem> getItems() {
        return items;
    }

    public void setItems(List<ZipDownloadItem> items) {
        this.items = items;
    }
}
