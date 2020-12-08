package com.changhong.sei.edm.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Collection;

/**
 * 实现功能：绑定业务实体文档信息请求
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-19 15:31
 */
@ApiModel(description = "绑定业务实体文档信息请求")
public class BindRequest implements Serializable {
    private static final long serialVersionUID = 6153285861490679417L;

    /**
     * 业务实体id
     */
    @ApiModelProperty(notes = "业务实体id", required = true)
    @NotBlank
    private String entityId;
    /**
     * 文档id集合
     */
    @ApiModelProperty(notes = "文档id集合", required = true)
    private Collection<String> documentIds;

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Collection<String> getDocumentIds() {
        return documentIds;
    }

    public void setDocumentIds(Collection<String> documentIds) {
        this.documentIds = documentIds;
    }
}
