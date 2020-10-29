package com.changhong.sei.search.service;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.dto.serach.PageInfo;
import com.changhong.sei.core.dto.serach.PageResult;
import com.changhong.sei.core.dto.serach.Search;
import com.changhong.sei.core.dto.serach.SearchFilter;
import com.changhong.sei.core.util.JsonUtils;
import com.changhong.sei.search.dto.ElasticSearch;
import com.changhong.sei.search.entity.ElasticEntity;
import com.changhong.sei.util.ConverterUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 00:03
 */
@Service
public class BaseElasticService {
    private static final Logger LOG = LoggerFactory.getLogger(BaseElasticService.class);

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 创建索引
     *
     * @param idxName 索引名称(indexName必须是小写，如果是大写在创建过程中会有错误)
     * @param idxSQL  索引描述
     */
    public ResultData<String> createIndex(String idxName, String idxSQL) {
        if (StringUtils.isBlank(idxName)) {
            return ResultData.fail("索引名不能为空.");
        } else {
            idxName = idxName.toLowerCase();
        }
        try {
            if (!this.indexExist(idxName)) {
                LOG.error(" idxName={} 已经存在,idxSql={}", idxName, idxSQL);
                return ResultData.fail("idxName=" + idxName + " 已经存在,idxSql=" + idxSQL);
            }
            CreateIndexRequest request = new CreateIndexRequest(idxName);
            buildSetting(request);
            request.mapping(idxSQL, XContentType.JSON);
//            request.settings() 手工指定Setting
            CreateIndexResponse res = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
            if (!res.isAcknowledged()) {
                return ResultData.fail("创建索引[" + idxName + "]失败");
            }
        } catch (Exception e) {
            LOG.error("创建索引异常", e);
            return ResultData.fail("创建索引异常");
        }
        return ResultData.success();
    }

    /**
     * 删除index
     */
    public ResultData<String> deleteIndex(String idxName) {
        if (StringUtils.isBlank(idxName)) {
            return ResultData.fail("索引名不能为空.");
        } else {
            idxName = idxName.toLowerCase();
        }
        try {
            if (!this.indexExist(idxName)) {
                LOG.error(" idxName={} 已经存在", idxName);
                return ResultData.fail("idxName[" + idxName + "]已经存在");
            }
            AcknowledgedResponse response = restHighLevelClient.indices().delete(new DeleteIndexRequest(idxName), RequestOptions.DEFAULT);
            if (response.isAcknowledged()) {
                return ResultData.success();
            } else {
                return ResultData.fail("删除索引[" + idxName + "]失败");
            }
        } catch (Exception e) {
            LOG.error("删除index异常", e);
            return ResultData.fail("删除index异常");
        }
    }

    /**
     * 制定配置项的判断索引是否存在，注意与 isExistsIndex 区别
     *
     * @param idxName index名
     */
    public boolean indexExist(String idxName) throws Exception {
        if (StringUtils.isBlank(idxName)) {
            throw new IllegalArgumentException("索引名不能为空.");
        } else {
            idxName = idxName.toLowerCase();
        }
        GetIndexRequest request = new GetIndexRequest(idxName);
        //TRUE-返回本地信息检索状态，FALSE-还是从主节点检索状态
        request.local(false);
        //是否适应被人可读的格式返回
        request.humanReadable(true);
        //是否为每个索引返回所有默认设置
        request.includeDefaults(false);
        //控制如何解决不可用的索引以及如何扩展通配符表达式,忽略不可用索引的索引选项，仅将通配符扩展为开放索引，并且不允许从通配符表达式解析任何索引
        request.indicesOptions(IndicesOptions.lenientExpandOpen());
        return restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
    }

    /**
     * 直接判断某个index是否存在
     *
     * @param idxName index名
     */
    public boolean isExistsIndex(String idxName) throws Exception {
        if (StringUtils.isBlank(idxName)) {
            throw new IllegalArgumentException("索引名不能为空.");
        } else {
            idxName = idxName.toLowerCase();
        }
        return restHighLevelClient.indices().exists(new GetIndexRequest(idxName), RequestOptions.DEFAULT);
    }

    /**
     * @param idxName index
     * @param entity  对象
     */
    public ResultData<String> save(String idxName, ElasticEntity entity) {
        if (StringUtils.isBlank(idxName)) {
            return ResultData.fail("索引名不能为空.");
        } else {
            idxName = idxName.toLowerCase();
        }
        IndexRequest request = new IndexRequest(idxName);
        String id = entity.getId();
        String data = JsonUtils.toJson(entity.getData());
        LOG.info("Data : id={},entity={}", id, data);
        request.id(id);
//        request.source(entity.getData(), XContentType.JSON);
        request.source(data, XContentType.JSON);
        try {
            IndexResponse response = restHighLevelClient.index(request, RequestOptions.DEFAULT);
            DocWriteResponse.Result result = response.getResult();
            if (DocWriteResponse.Result.CREATED == result || DocWriteResponse.Result.UPDATED == result) {
                return ResultData.success(result.getLowercase());
            }
            return ResultData.fail(result.getLowercase());
        } catch (Exception e) {
            LOG.error("保存异常", e);
            return ResultData.fail("保存异常.");
        }
    }

    /**
     * 批量写入数据
     *
     * @param idxName index
     * @param list    带插入列表
     */
    public ResultData<String> batchSave(String idxName, Collection<ElasticEntity> list) {
        if (StringUtils.isBlank(idxName)) {
            return ResultData.fail("索引名不能为空.");
        } else {
            idxName = idxName.toLowerCase();
        }
        BulkRequest request = new BulkRequest();
        final String indexName = idxName;
        list.forEach(item -> request.add(new IndexRequest(indexName).id(item.getId())
                .source(JsonUtils.toJson(item.getData()), XContentType.JSON)));
        try {
            BulkResponse response = restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
            if (!response.hasFailures()) {
                return ResultData.success();
            }
            return ResultData.fail(response.buildFailureMessage());
        } catch (Exception e) {
            LOG.error("批量写入数据异常", e);
            return ResultData.fail("批量写入数据异常.");
        }
    }

    /**
     * 批量插入数据
     *
     * @param idxName index
     * @param list    带插入列表
     */
    public ResultData<String> batchSaveObj(String idxName, Collection<ElasticEntity> list) {
        if (StringUtils.isBlank(idxName)) {
            return ResultData.fail("索引名不能为空.");
        } else {
            idxName = idxName.toLowerCase();
        }
        final String indexName = idxName;
        BulkRequest request = new BulkRequest();
        list.forEach(item -> request.add(new IndexRequest(indexName).id(item.getId())
                .source(item.getData(), XContentType.JSON)));
        try {
            BulkResponse response = restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
            if (!response.hasFailures()) {
                return ResultData.success();
            }
            return ResultData.fail(response.buildFailureMessage());
        } catch (Exception e) {
            LOG.error("批量写入数据异常", e);
            return ResultData.fail("批量写入数据异常.");
        }
    }

    /**
     * @param idxName index
     * @param entity  对象
     */
    public ResultData<String> deleteOne(String idxName, ElasticEntity entity) {
        if (StringUtils.isBlank(idxName)) {
            return ResultData.fail("索引名不能为空.");
        } else {
            idxName = idxName.toLowerCase();
        }
        DeleteRequest request = new DeleteRequest(idxName);
        request.id(entity.getId());
        try {
            DeleteResponse response = restHighLevelClient.delete(request, RequestOptions.DEFAULT);

            if (DocWriteResponse.Result.DELETED == response.getResult()) {
                return ResultData.success();
            } else {
                LOG.error(response.toString());
                return ResultData.fail(response.getResult().getLowercase());
            }
        } catch (Exception e) {
            LOG.error("删除数据异常", e);
            return ResultData.fail("按ID[" + entity.getId() + "]删除Index[" + idxName + "]的数据异常");
        }
    }

    /**
     * 批量删除
     *
     * @param idxName index
     * @param idList  待删除列表
     */
    public <T> ResultData<String> deleteBatch(String idxName, Collection<T> idList) {
        if (StringUtils.isBlank(idxName)) {
            return ResultData.fail("索引名不能为空.");
        } else {
            idxName = idxName.toLowerCase();
        }
        final String indexName = idxName;
        BulkRequest request = new BulkRequest();
        idList.forEach(item -> request.add(new DeleteRequest(indexName, item.toString())));
        try {
            BulkResponse response = restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
            if (!response.hasFailures()) {
                return ResultData.success();
            } else {
                LOG.error(response.buildFailureMessage());
                return ResultData.fail(response.buildFailureMessage());
            }
        } catch (Exception e) {
            LOG.error("删除数据异常", e);
            return ResultData.fail("删除Index[" + idxName + "]的数据异常");
        }
    }

    /**
     * 按条件删除
     * 最大操作量为10000个
     */
    public ResultData<String> deleteByQuery(String idxName, QueryBuilder builder) {
        if (StringUtils.isBlank(idxName)) {
            return ResultData.fail("索引名不能为空.");
        } else {
            idxName = idxName.toLowerCase();
        }
        DeleteByQueryRequest request = new DeleteByQueryRequest(idxName);
        request.setQuery(builder);
        //设置批量操作数量,最大为10000
        request.setBatchSize(10000);
        request.setConflicts("proceed");
        try {
            restHighLevelClient.deleteByQuery(request, RequestOptions.DEFAULT);
            return ResultData.success();
        } catch (Exception e) {
            LOG.error("按条件删除异常", e);
            return ResultData.fail("[" + idxName + "]按条件删除异常");
        }
    }

    /**
     * 查询
     *
     * @param idxName    索引名
     * @param properties 属性
     * @param keyword    关键字
     * @return java.util.List<T>
     */
    public ResultData<List<HashMap<String, Object>>> search(String idxName, String[] properties, String keyword) {
        MultiMatchQueryBuilder queryBuilder = QueryBuilders.multiMatchQuery(keyword, properties);
        SearchSourceBuilder searchSourceBuilder = this.initSearchSourceBuilder(queryBuilder);
        // 初始化高亮设置
        initHighlightBuilder(properties, searchSourceBuilder);
        return search(idxName, searchSourceBuilder);
    }

    /**
     * 查询
     *
     * @param search 查询参数
     * @return java.util.List<T>
     */
    public ResultData<List<HashMap<String, Object>>> search(ElasticSearch search) {
        SearchSourceBuilder searchSourceBuilder = this.initSearchSourceBuilder(buildBoolQueryBuilder(search));
        // 初始化高亮设置
        initHighlightBuilder(search.getHighlightFields(), searchSourceBuilder);

        ResultData<List<HashMap<String, Object>>> data = this.search(search.getIdxName(), searchSourceBuilder);
        return data;

    }

    /**
     * 查询
     *
     * @param search 查询参数
     * @return java.util.List<T>
     */
    @SuppressWarnings("unchecked")
    public ResultData<PageResult<HashMap<String, Object>>> findByPage(ElasticSearch search) {
        String idxName = search.getIdxName();
        if (StringUtils.isBlank(idxName)) {
            return ResultData.fail("索引名不能为空.");
        } else {
            idxName = idxName.toLowerCase();
        }
        PageInfo pageInfo = search.getPageInfo();
        if (Objects.isNull(pageInfo)) {
            pageInfo = new PageInfo();
        }
        try {
            SearchSourceBuilder searchSourceBuilder = this.initSearchSourceBuilder(buildBoolQueryBuilder(search),
                    pageInfo.getPage() - 1, pageInfo.getRows(), 120);

            // 初始化高亮设置
            initHighlightBuilder(search.getHighlightFields(), searchSourceBuilder);

            SearchRequest request = new SearchRequest(idxName);

            request.source(searchSourceBuilder);

            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            SearchHits searchHits = response.getHits();
            // 获取总数
            long total = searchHits.getTotalHits().value;

            // 计算总页数
            int totalPage = total % pageInfo.getRows() == 0 ? (int) (total / pageInfo.getRows()) : ((int) (total / pageInfo.getRows()) + 1);

            PageResult<HashMap<String, Object>> pageResult = new PageResult<>();
            pageResult.setPage(pageInfo.getPage());
            pageResult.setRecords(total);
            pageResult.setTotal(totalPage);

            SearchHit[] hits = searchHits.getHits();
            pageResult.setRows(processData(hits));

            return ResultData.success(pageResult);
        } catch (Exception e) {
            LOG.error("查询数据异常，idxName={}", search.getIdxName(), e);
        }
        return ResultData.fail("查询数据.");
    }

    /**
     * 查询
     *
     * @param idxName index
     * @param builder 查询参数
     * @return java.util.List<T>
     */
    public ResultData<List<HashMap<String, Object>>> search(String idxName, SearchSourceBuilder builder) {
        if (StringUtils.isBlank(idxName)) {
            return ResultData.fail("索引名不能为空.");
        } else {
            idxName = idxName.toLowerCase();
        }
        SearchRequest request = new SearchRequest(idxName);
        request.source(builder);
        try {
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            SearchHits searchHits = response.getHits();

            SearchHit[] hits = searchHits.getHits();
//            List<Map<String, Object>> res = new ArrayList<>(hits.length);
//            for (SearchHit hit : hits) {
////                // 结果的Index
////                String index = hit.getIndex();
////                // 结果的type
////                String type = hit.getType();
////                // 结果的ID
////                String id = hit.getId();
////                // 结果的评分
////                float score = hit.getScore();
//                // 查询的结果 JSON字符串形式
////                res.add(JsonUtils.fromJson(hit.getSourceAsString(), HashMap.class));
//                // 查询的结果 Map的形式
//                res.add(hit.getSourceAsMap());
//            }
            return ResultData.success(processData(hits));
        } catch (Exception e) {
            LOG.error("查询数据异常，idxName={}", idxName, e);
        }
        return ResultData.fail("查询数据.");
    }

    /**
     * 默认分片设置
     */
    private void buildSetting(CreateIndexRequest request) {
        request.settings(Settings.builder()
                // 分片数
                .put("index.number_of_shards", 3)
                // 副本数
                .put("index.number_of_replicas", 2));
    }

    /**
     * @param search 设置查询对象
     */
    private BoolQueryBuilder buildBoolQueryBuilder(Search search) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

        String keyword = search.getQuickSearchValue();
        if (StringUtils.isNotBlank(keyword)) {
            Collection<String> list = search.getQuickSearchProperties();
            if (CollectionUtils.isNotEmpty(list)) {
                String[] fields = list.toArray(new String[0]);

                queryBuilder.must(QueryBuilders.multiMatchQuery(keyword, fields));
            }
        }

        /*
        关键词	        keyword类型	text类型	                                                                    是否支持分词
        term	        完全匹配	    查询条件必须都是text分词中的，且不能多余，多个分词时必须连续，顺序不能颠倒。	            否
        match	        完全匹配	    match分词结果和text的分词结果有相同的即可，不考虑顺序	                            是
        match_phrase	完全匹配	    match_phrase的分词结果必须在text字段分词中都包含，而且顺序必须相同，而且必须都是连续的。	是
        query_string	完全匹配	    query_string中的分词结果至少有一个在text字段的分词结果中，不考虑顺序	                是
         */
        Object matchValue;
        List<SearchFilter> filters = search.getFilters();
        if (Objects.nonNull(filters)) {
            for (SearchFilter filter : filters) {
                matchValue = filter.getValue();
                switch (filter.getOperator()) {
                    case EQ:
                        // 精准查询
                        queryBuilder.must(QueryBuilders.matchPhraseQuery(filter.getFieldName(), matchValue));
                        break;
                    case NE:
                        queryBuilder.mustNot(QueryBuilders.matchQuery(filter.getFieldName(), matchValue));
                        break;
                    case LK:
                    case LLK:
                    case RLK:
                        // 模糊查询
//                        queryBuilder.must(QueryBuilders.queryStringQuery(String.valueOf(matchValue)));
                        queryBuilder.must(QueryBuilders.matchQuery(filter.getFieldName(), matchValue));
                        break;
                    case GE:
                        if (StringUtils.equalsIgnoreCase("timestamp", filter.getFieldName())) {
                            try {
                                Date date = ConverterUtils.getAsDate(matchValue);
                                queryBuilder.must(QueryBuilders.rangeQuery(filter.getFieldName()).gte(date.getTime()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            queryBuilder.must(QueryBuilders.rangeQuery(filter.getFieldName()).gte(matchValue));
                        }
                        break;
                    case GT:
                        if (StringUtils.equalsIgnoreCase("timestamp", filter.getFieldName())) {
                            try {
                                Date date = ConverterUtils.getAsDate(matchValue);
                                queryBuilder.must(QueryBuilders.rangeQuery(filter.getFieldName()).gt(date.getTime()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            queryBuilder.must(QueryBuilders.rangeQuery(filter.getFieldName()).gt(matchValue));
                        }
                        break;
                    case LE:
                        if (StringUtils.equalsIgnoreCase("timestamp", filter.getFieldName())) {
                            try {
                                Date date = ConverterUtils.getAsDate(matchValue);
                                queryBuilder.must(QueryBuilders.rangeQuery(filter.getFieldName()).lte(date.getTime()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            queryBuilder.must(QueryBuilders.rangeQuery(filter.getFieldName()).lte(matchValue));
                        }
                        break;
                    case LT:
                        if (StringUtils.equalsIgnoreCase("timestamp", filter.getFieldName())) {
                            try {
                                Date date = ConverterUtils.getAsDate(matchValue);
                                queryBuilder.must(QueryBuilders.rangeQuery(filter.getFieldName()).lt(date.getTime()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            queryBuilder.must(QueryBuilders.rangeQuery(filter.getFieldName()).lt(matchValue));
                        }
                        break;
                    case BT:
                        Assert.notNull(matchValue, "Match value must be not null");
                        Assert.isTrue(matchValue.getClass().isArray(), "Match value must be array");
                        Object[] matchValues = (Object[]) matchValue;
                        Assert.isTrue(matchValues.length == 2, "Match value must have two value");
                        // 对日期特殊处理：一般用于区间日期的结束时间查询,如查询2012-01-01之前,一般需要显示2010-01-01当天及以前的数据,
                        // 而数据库一般存有时分秒,因此需要特殊处理把当前日期+1天,转换为<2012-01-02进行查询
                        if (StringUtils.equalsIgnoreCase("timestamp", filter.getFieldName())) {
                            try {
                                Date date0 = ConverterUtils.getAsDate(matchValues[0]);
                                Date date1 = ConverterUtils.getAsDate(matchValues[1]);
                                queryBuilder.must(QueryBuilders.rangeQuery(filter.getFieldName()).from(date0.getTime()).to(date1.getTime()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            queryBuilder.must(QueryBuilders.rangeQuery(filter.getFieldName()).gte(matchValues[0]).lte(matchValues[1]));
                        }
                        break;
                    default:
                        queryBuilder.must(QueryBuilders.queryStringQuery(String.valueOf(matchValue)));
                }
            }
        }
        return queryBuilder;
    }

    /**
     * @param queryBuilder 设置查询对象
     * @param from         设置from选项，确定要开始搜索的结果索引。 默认为0。
     * @param size         设置大小选项，确定要返回的搜索匹配数。 默认为10。
     */
    private SearchSourceBuilder initSearchSourceBuilder(QueryBuilder queryBuilder, int from, int size, int timeout) {
        //使用默认选项创建 SearchSourceBuilder 。
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //设置查询对象。可以使任何类型的 QueryBuilder
        sourceBuilder.query(queryBuilder);
        //设置from选项，确定要开始搜索的结果索引。 默认为0。
        sourceBuilder.from(from);
        //设置大小选项，确定要返回的搜索匹配数。 默认为10。
        sourceBuilder.size(size);
        sourceBuilder.timeout(new TimeValue(timeout, TimeUnit.SECONDS));
        return sourceBuilder;
    }

    private SearchSourceBuilder initSearchSourceBuilder(QueryBuilder queryBuilder) {
        return initSearchSourceBuilder(queryBuilder, 0, 10, 60);
    }

    /**
     * 初始化设置高亮字段
     */
    private void initHighlightBuilder(String[] fields, SearchSourceBuilder searchSourceBuilder) {
        if (Objects.nonNull(fields)) {
            Set<HighlightBuilder.Field> highlightFields = new HashSet<>();
            for (String field : fields) {
                highlightFields.add(new HighlightBuilder.Field(field));
            }
            // 高亮查询
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            // 高亮前缀
            highlightBuilder.preTags("<em>");
            // 高亮后缀
            highlightBuilder.postTags("</em>");
            // 高亮字段
            highlightBuilder.fields().addAll(highlightFields);
            // 添加高亮查询条件到搜索源
            searchSourceBuilder.highlighter(highlightBuilder);
        }
    }

    @SuppressWarnings("unchecked")
    private List<HashMap<String, Object>> processData(SearchHit[] hits) {
        List<HashMap<String, Object>> results = new ArrayList<>();
        if (Objects.isNull(hits) || hits.length == 0) {
            return results;
        }
        // 源文档内容
        HashMap<String, Object> sourceAsMap;
        StringBuilder stringBuffer = new StringBuilder();
        for (SearchHit hit : hits) {
            // 文档的主键
            String id = hit.getId();
            // 源文档内容
            sourceAsMap = (HashMap) hit.getSourceAsMap();
            // 追加文档的主键
            sourceAsMap.put("_id", id);

            // 获取高亮查询的内容。如果存在，则替换原来的name
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null && highlightFields.size() > 0) {
                for (Map.Entry<String, HighlightField> entry : highlightFields.entrySet()) {
                    HighlightField nameField = entry.getValue();
                    if (nameField != null) {
                        Text[] fragments = nameField.getFragments();
                        stringBuffer.delete(0, stringBuffer.length());
                        for (Text str : fragments) {
                            stringBuffer.append(str.string());
                        }
                        sourceAsMap.put(entry.getKey(), stringBuffer.toString());
                    }
                }
            }
            results.add(sourceAsMap);
        }

        return results;
    }
}
