package com.changhong.sei.edm.manager.entity;

import com.changhong.sei.core.entity.BaseEntity;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 实现功能：文档信息
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-03 15:44
 */
@Entity
@Table(name = "edm_business_document")
@DynamicInsert
@DynamicUpdate
public class BusinessDocument extends BaseEntity {
    private static final long serialVersionUID = 1189243298194997374L;

    /**
     * 业务实体id
     */
    @Column(name = "entity_id", length = 100)
    private String entityId;
    /**
     * 文件描述
     */
    @Column(name = "doc_id")
    private String docId;

    public BusinessDocument() {
    }

    public BusinessDocument(String entityId, String docId) {
        this.docId = docId;
        this.entityId = entityId;
    }


    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }
}
