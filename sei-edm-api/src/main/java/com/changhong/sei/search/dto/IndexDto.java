package com.changhong.sei.search.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * 实现功能：创建索引模板，用于解析为JSON 格式
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 00:17
 */
public class IndexDto implements Serializable {
    private static final long serialVersionUID = 4822413768914555331L;

    /**
     * idxName : idx_location
     * idxSql : {"dynamic":false,"properties":{"id":{"type":"long"},"code":{"type":"text","index":true},"name":{"type":"text","index":true,"analyzer":"ik_max_word"},"url":{"type":"text","index":true}}}
     */
    private String idxName;
    private IdxSql idxSql;

    public String getIdxName() {
        return idxName;
    }

    public void setIdxName(String idxName) {
        this.idxName = idxName;
    }

    public IdxSql getIdxSql() {
        return idxSql;
    }

    public void setIdxSql(IdxSql idxSql) {
        this.idxSql = idxSql;
    }

    public static class IdxSql {
        /**
         * dynamic : false
         * properties : {"id":{"type":"long"},"code":{"type":"text","index":true},"name":{"type":"text","index":true,"analyzer":"ik_max_word"},"url":{"type":"text","index":true}}
         */
        private boolean dynamic = false;
        private Map<String, Map<String, Object>> properties;

        public boolean isDynamic() {
            return dynamic;
        }

        public void setDynamic(boolean dynamic) {
            this.dynamic = dynamic;
        }

        public Map<String, Map<String, Object>> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Map<String, Object>> properties) {
            this.properties = properties;
        }
    }
}
