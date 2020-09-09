package com.changhong.sei.edm.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-08 16:07
 */
@ApiModel(description = "文件分片上传")
public class FileChunkResponse implements Serializable {
    private static final long serialVersionUID = -5924767220212635679L;
    /**
     * 文件分块标识
     */
    @ApiModelProperty(notes = "文件分块标识", required = true)
    private String docId;
    /**
     * 当前文件块，从1开始
     */
    @NotNull
    @ApiModelProperty(notes = "当前文件块，从1开始", required = true)
    private Integer chunkNumber;
    /**
     * 当前分块大小
     */
    @NotNull
    @ApiModelProperty(notes = "当前分块大小", required = true)
    private Long currentChunkSize;
    /**
     * 分块大小
     */
    @NotNull
    @ApiModelProperty(notes = "分块大小", required = true)
    private Long chunkSize;
    /**
     * 总大小
     */
    @NotNull
    @ApiModelProperty(notes = "总大小", required = true)
    private Long totalSize;
    /**
     * 总块数
     */
    @NotNull
    @ApiModelProperty(notes = "总块数", required = true)
    private Integer totalChunks;
    /**
     * 原整体文件MD5
     */
    @NotNull
    @ApiModelProperty(notes = "原整体文件MD5", required = true)
    private String fileMd5;

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public Integer getChunkNumber() {
        return chunkNumber;
    }

    public void setChunkNumber(Integer chunkNumber) {
        this.chunkNumber = chunkNumber;
    }

    public Long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Long getCurrentChunkSize() {
        return currentChunkSize;
    }

    public void setCurrentChunkSize(Long currentChunkSize) {
        this.currentChunkSize = currentChunkSize;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

}
