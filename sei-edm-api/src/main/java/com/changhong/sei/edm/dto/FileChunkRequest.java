package com.changhong.sei.edm.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-08 16:07
 */
@ApiModel(description = "文件分片上传")
public class FileChunkRequest extends FileChunkResponse implements Serializable {
    private static final long serialVersionUID = -5924767220212635679L;

    @ApiModelProperty(notes = "文件", required = true)
    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
