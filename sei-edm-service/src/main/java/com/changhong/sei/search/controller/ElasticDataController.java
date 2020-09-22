package com.changhong.sei.search.controller;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.search.api.ElasticDataApi;
import com.changhong.sei.search.common.util.ElasticUtil;
import com.changhong.sei.search.dto.ElasticDataDto;
import com.changhong.sei.search.dto.QueryDto;
import com.changhong.sei.search.entity.ElasticEntity;
import com.changhong.sei.search.service.BaseElasticService;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 00:42
 */
@RestController
@Api(value = "ElasticDataApi", tags = "ElasticSearch的数据管理，提供对外查询、删除和新增功能")
public class ElasticDataController implements ElasticDataApi {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticDataController.class);

    @Autowired
    private BaseElasticService baseElasticService;

    /**
     * 新增数据
     */
    @Override
    public ResultData<String> add(ElasticDataDto elasticDataDto) {
        try {
            if (StringUtils.isEmpty(elasticDataDto.getIdxName())) {
                LOG.warn("索引为空");
                return ResultData.fail("索引为空，不允许提交");
            }
            baseElasticService.insertOrUpdateOne(elasticDataDto.getIdxName(),
                    new ElasticEntity(elasticDataDto.getId(), elasticDataDto.getData()));
        } catch (Exception e) {
            LOG.error("插入数据异常，metadataVo={},异常信息={}", elasticDataDto.toString(), e.getMessage());
            return ResultData.fail("插入数据异常");
        }
        return ResultData.success();
    }

    /**
     * 删除
     */
    @Override
    public ResultData<String> delete(ElasticDataDto elasticDataDto) {
        try {
            if (!StringUtils.isNotEmpty(elasticDataDto.getIdxName())) {
                LOG.warn("索引为空");
                return ResultData.fail("索引为空，不允许提交");
            }
            ElasticEntity elasticEntity = new ElasticEntity();
            elasticEntity.setId(elasticDataDto.getId());
            elasticEntity.setData(elasticDataDto.getData());
            baseElasticService.deleteOne(elasticDataDto.getIdxName(), elasticEntity);
        } catch (Exception e) {
            LOG.error("删除数据失败");
            return ResultData.fail("删除数据失败");
        }
        return ResultData.success();

    }

    /**
     * @param queryDto 查询实体对象
     */
    @Override
    public ResultData<List<?>> get(QueryDto queryDto) {
        if (StringUtils.isEmpty(queryDto.getIdxName())) {
            LOG.warn("索引为空");
            return ResultData.fail("索引为空，不允许提交");
        }

        try {
            Map<String, Object> params = queryDto.getQuery().get("match");
            Set<String> keys = params.keySet();
            MatchQueryBuilder queryBuilders = null;
            for (String ke : keys) {
                queryBuilders = QueryBuilders.matchQuery(ke, params.get(ke));
            }
            if (null != queryBuilders) {
                SearchSourceBuilder searchSourceBuilder = ElasticUtil.initSearchSourceBuilder(queryBuilders);
                List<?> data = baseElasticService.search(queryDto.getIdxName(), searchSourceBuilder);
                return ResultData.success(data);
            }
        } catch (Exception e) {
            LOG.error("查询数据异常，metadataVo={},异常信息={}", queryDto.toString(), e.getMessage());
        }
        return ResultData.fail("服务忙，请稍后再试");
    }
}
