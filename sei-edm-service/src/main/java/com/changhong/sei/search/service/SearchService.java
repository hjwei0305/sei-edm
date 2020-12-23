package com.changhong.sei.search.service;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.dto.serach.PageResult;
import com.changhong.sei.search.dto.ElasticSearch;
import com.changhong.sei.search.entity.ElasticEntity;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-12-23 13:15
 */
public interface SearchService {
    /**
     * 创建索引
     *
     * @param idxName 索引名称(indexName必须是小写，如果是大写在创建过程中会有错误)
     * @param idxSQL  索引描述
     */
    ResultData<String> createIndex(String idxName, String idxSQL);

    /**
     * 删除index
     */
    ResultData<String> deleteIndex(String idxName);

    /**
     * 制定配置项的判断索引是否存在，注意与 isExistsIndex 区别
     *
     * @param idxName index名
     */
    boolean indexExist(String idxName) throws Exception;

    /**
     * 直接判断某个index是否存在
     *
     * @param idxName index名
     */
    boolean isExistsIndex(String idxName) throws Exception;

    /**
     * @param idxName index
     * @param entity  对象
     */
    ResultData<String> save(String idxName, ElasticEntity entity);

    /**
     * 批量写入数据
     *
     * @param idxName index
     * @param list    带插入列表
     */
    ResultData<String> batchSave(String idxName, Collection<ElasticEntity> list);

    /**
     * 批量插入数据
     *
     * @param idxName index
     * @param list    带插入列表
     */
    ResultData<String> batchSaveObj(String idxName, Collection<ElasticEntity> list);

    /**
     * @param idxName index
     * @param entity  对象
     */
    ResultData<String> deleteOne(String idxName, ElasticEntity entity);

    /**
     * 批量删除
     *
     * @param idxName index
     * @param idList  待删除列表
     */
    <T> ResultData<String> deleteBatch(String idxName, Collection<T> idList);

    /**
     * 按条件删除
     * 最大操作量为10000个
     */
    ResultData<String> deleteByQuery(String idxName, QueryBuilder builder);

    /**
     * 查询
     *
     * @param idxName    索引名
     * @param properties 属性
     * @param keyword    关键字
     * @return java.util.List<T>
     */
    ResultData<List<HashMap<String, Object>>> search(String idxName, String[] properties, String keyword);

    /**
     * 查询
     *
     * @param search 查询参数
     * @return java.util.List<T>
     */
    ResultData<List<HashMap<String, Object>>> search(ElasticSearch search);

    /**
     * 查询
     *
     * @param search 查询参数
     * @return java.util.List<T>
     */
    ResultData<PageResult<HashMap<String, Object>>> findByPage(ElasticSearch search);

    /**
     * 查询
     *
     * @param idxName index
     * @param builder 查询参数
     * @return java.util.List<T>
     */
    ResultData<List<HashMap<String, Object>>> search(String idxName, SearchSourceBuilder builder);
}
