package com.changhong.sei.search.dto;

import java.util.Map;

/**
 * 实现功能：查询dto对象
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 00:19
 */
public class QueryDto {
    /**
     * 索引名
     */
    private String idxName;
    /**
     * 具体条件
     */
    private Map<String, Map<String, Object>> query;

    public String getIdxName() {
        return idxName;
    }

    public void setIdxName(String idxName) {
        this.idxName = idxName;
    }

    public Map<String, Map<String, Object>> getQuery() {
        return query;
    }

    public void setQuery(Map<String, Map<String, Object>> query) {
        this.query = query;
    }
}
