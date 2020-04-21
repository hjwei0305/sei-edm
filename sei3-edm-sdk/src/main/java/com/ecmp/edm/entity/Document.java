package com.ecmp.edm.entity;

import java.io.InputStream;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：文档(包含信息和数据)
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017-07-11 14:29      王锦光(wangj)                新建
 * <p/>
 * *************************************************************************************************
 */
public class Document {
    /**
     * 文档信息
     */
    private DocumentInfo info;
    /**
     * 文档数据
     */
    private InputStream stream;

    /**
     * 构造函数
     *
     * @param info   文档信息
     * @param stream 文档数据
     */
    public Document(DocumentInfo info, InputStream stream) {
        this.info = info;
        this.stream = stream;
    }

    public DocumentInfo getInfo() {
        return info;
    }

    public InputStream getStream() {
        return stream;
    }
}
