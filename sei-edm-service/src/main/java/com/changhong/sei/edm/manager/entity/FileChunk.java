package com.changhong.sei.edm.manager.entity;

import com.changhong.sei.core.entity.BaseEntity;
import com.changhong.sei.edm.dto.DocumentType;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 实现功能： 文件块
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-08 15:57
 */
@Entity
@Table(name = "edm_file_chunk")
@DynamicInsert
@DynamicUpdate
public class FileChunk extends BaseEntity {
    private static final long serialVersionUID = -2978085016335609068L;
    /**
     * 当前文件块标识
     */
    @Column(name = "doc_id", length = 200, nullable = false)
    private String docId;
    /**
     * 当前文件块，从1开始
     */
    @Column(name = "chunk_number", nullable = false)
    private Integer chunkNumber;
    /**
     * 分块大小
     */
    @Column(name = "chunk_size", nullable = false)
    private Long chunkSize;
    /**
     * 当前分块大小
     */
    @Column(name = "current_chunk_size", nullable = false)
    private Long currentChunkSize;
    /**
     * 总大小
     */
    @Column(name = "total_size", nullable = false)
    private Long totalSize;
    /**
     * 总块数
     */
    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;
    /**
     * 原整体文件MD5
     */
    @Column(name = "file_md5")
    private String fileMd5;
    /**
     * 文件名
     */
    @Column(name = "file_name", length = 200)
    private String filename;
    /**
     * 上传时间
     */
    @Column(name = "uploaded_time")
    private LocalDateTime uploadedTime;

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

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public LocalDateTime getUploadedTime() {
        return uploadedTime;
    }

    public void setUploadedTime(LocalDateTime uploadedTime) {
        this.uploadedTime = uploadedTime;
    }
}
