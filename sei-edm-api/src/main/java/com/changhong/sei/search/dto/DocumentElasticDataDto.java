package com.changhong.sei.search.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 实现功能：ES业务文档实体Dto
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 00:15
 */
@ApiModel(description = "ES业务文档实体Dto")
public class DocumentElasticDataDto extends ElasticDataDto implements Serializable {
    private static final long serialVersionUID = -7357129504573889779L;

    /**
     * 文档id
     */
    @NotNull
    @ApiModelProperty(notes = "文档Id", required = true)
    private String[] docIds;

    public String[] getDocIds() {
        return docIds;
    }

    public void setDocIds(String[] docIds) {
        this.docIds = docIds;
    }
}
