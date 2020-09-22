package com.changhong.sei.search.dto;

import com.changhong.sei.core.dto.serach.Search;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * 实现功能：ES查询dto对象
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 00:19
 */
@ApiModel(description = "ES查询dto对象")
public class ElasticSearch extends Search {
    private static final long serialVersionUID = 6339284532868826189L;
    /**
     * 索引名
     */
    @NotBlank
    @ApiModelProperty(notes = "索引名", required = true)
    private String idxName;

    @ApiModelProperty(notes = "高亮字段.不设置视为不开启")
    private String[] highlightFields;

    public String getIdxName() {
        return idxName;
    }

    public void setIdxName(String idxName) {
        this.idxName = idxName;
    }

    public String[] getHighlightFields() {
        return highlightFields;
    }

    public void setHighlightFields(String[] highlightFields) {
        this.highlightFields = highlightFields;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ElasticSearch.class.getSimpleName() + "[", "]")
                .add("idxName='" + idxName + "'")
                .add("highlightFields=" + Arrays.toString(highlightFields))
                .toString();
    }
}
