package com.changhong.sei.edm.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

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
    @ApiModelProperty(notes = "文件分块标识")
    private String docId;
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
     * 上传状态
     */
    @ApiModelProperty(notes = "上传状态")
    private UploadEnum uploadState;

    /**
     * 已上传完成的文件块
     */
    @ApiModelProperty(notes = "已上传完成的文件块")
    private List<FileChunkDto> chunks;

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public Long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
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

    public UploadEnum getUploadState() {
        return uploadState;
    }

    public void setUploadState(UploadEnum uploadState) {
        this.uploadState = uploadState;
    }

    public List<FileChunkDto> getChunks() {
        return chunks;
    }

    public void setChunks(List<FileChunkDto> chunks) {
        this.chunks = chunks;
    }

    public enum UploadEnum {
        /**
         * 不存在
         */
        none,
        /**
         * 未完成
         */
        undone,
        /**
         * 已完成
         */
        completed
    }
}
