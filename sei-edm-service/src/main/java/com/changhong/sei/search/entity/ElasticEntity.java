package com.changhong.sei.search.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 实现功能：数据存储对象
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 00:02
 */
public class ElasticEntity implements Serializable {

    private static final long serialVersionUID = -3183847008469743794L;

    /**
     * 主键标识，用户ES持久化
     */
    private String id;

    /**
     * JSON对象，实际存储数据
     */
    private Map<String, Object> data;

    public ElasticEntity() {
    }

    public ElasticEntity(String id) {
        this.id = id;
    }

    public ElasticEntity(String id, Map<String, Object> data) {
        this.id = id;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getData() {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put("id", getId());
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
