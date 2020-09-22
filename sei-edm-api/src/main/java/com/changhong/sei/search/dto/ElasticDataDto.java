package com.changhong.sei.search.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Map;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 00:15
 */
@ApiModel(description = "ES业务实体Dto")
public class ElasticDataDto implements Serializable {
    private static final long serialVersionUID = -7357129504573889779L;

    /**
     * 索引名
     */
    @NotBlank
    @ApiModelProperty(notes = "索引名", required = true)
    private String idxName;

    /**
     * 主键标识，用户ES持久化
     */
    @ApiModelProperty(notes = "主键标识，用户ES持久化")
    private String id;

    /**
     * JSON对象，实际存储数据
     */
    @ApiModelProperty(notes = "JSON对象，实际存储数据")
    private Map<String, Object> data;

    public String getIdxName() {
        return idxName;
    }

    public void setIdxName(String idxName) {
        this.idxName = idxName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
