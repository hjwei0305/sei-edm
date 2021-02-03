package com.changhong.sei.edm.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-03-06 11:11
 */
@ApiModel(description = "文件下载")
public class DownloadRequest implements Serializable {
    private static final long serialVersionUID = 8539065361214753205L;

    @ApiModelProperty(notes = "业务实体id")
    private String entityId;
    // 文档id
    @ApiModelProperty(notes = "文档id")
    private Set<String> docIds;
    /**
     * 文件名（包括后缀）
     */
    @ApiModelProperty(notes = "文件名（包括后缀）")
    private String fileName;

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Set<String> getDocIds() {
        return docIds;
    }

    public void setDocIds(Set<String> docIds) {
        this.docIds = docIds;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
