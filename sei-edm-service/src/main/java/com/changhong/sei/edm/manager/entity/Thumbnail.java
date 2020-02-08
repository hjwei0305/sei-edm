package com.changhong.sei.edm.manager.entity;

import com.changhong.sei.core.entity.BaseEntity;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-06 14:58
 */
@Entity
@Table(name = "edm_thumbnail")
@DynamicInsert
@DynamicUpdate
public class Thumbnail extends BaseEntity {
    private static final long serialVersionUID = -8179348685747262031L;
    public static final String FIELD_DOC_ID = "docId";
    /**
     * docId
     */
    @Column(name = "doc_id", length = 100, unique = true)
    private String docId;
    /**
     * 文件名
     */
    @Column(name = "file_name", length = 100)
    private String fileName;
    /**
     * 缩略图数据
     */
    @Lob
    @Column(name = "data")
    private byte[] image;

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }
}
