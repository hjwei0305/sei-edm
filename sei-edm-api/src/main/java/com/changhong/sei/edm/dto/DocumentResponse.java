package com.changhong.sei.edm.dto;

import io.swagger.annotations.ApiModel;

import java.io.Serializable;

/**
 * 实现功能：文档(包含信息和数据)
 */
@ApiModel(description = "文档信息")
public class DocumentResponse extends DocumentDto implements Serializable {
    private static final long serialVersionUID = -7003748738807976561L;

    /**
     * 获取文件大小（K或M）
     */
    public String getFileSize() {
        String fileSize;
        long length = getSize();
        if (length == 0) {
            return "0K";
        }
        if (length < 1024 * 1024) {
            long ksize = length / 1024;
            if (ksize == 0) {
                ksize = 1;
            }
            fileSize = String.format("%dK", ksize);
        } else {
            long msize = length / (1024 * 1024);
            fileSize = String.format("%dM", msize);
        }
        return fileSize;
    }
}
