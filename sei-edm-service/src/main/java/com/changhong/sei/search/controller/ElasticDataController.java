package com.changhong.sei.search.controller;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.dto.serach.PageResult;
import com.changhong.sei.search.api.ElasticDataApi;
import com.changhong.sei.search.dto.DocumentElasticDataDto;
import com.changhong.sei.search.dto.ElasticDataDto;
import com.changhong.sei.search.dto.ElasticSearch;
import com.changhong.sei.search.entity.ElasticEntity;
import com.changhong.sei.search.service.AsyncDocumentContextService;
import com.changhong.sei.search.service.BaseElasticService;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;

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
    @Autowired
    private AsyncDocumentContextService asyncDocumentContextService;

    /**
     * 新增业务文档数据
     */
    @Override
    public ResultData<String> addDoc(@Valid DocumentElasticDataDto elasticDataDto) {
        asyncDocumentContextService.recognizeAndSaveElastic(elasticDataDto);
        return ResultData.success();
    }

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
            baseElasticService.save(elasticDataDto.getIdxName(),
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
     * @param idxName    索引名
     * @param properties 查询字段.多个用英文逗号分隔
     * @param keyword    查询关键字
     */
    @Override
    public ResultData<List<HashMap<String, Object>>> findByProperty(String idxName, String properties, String keyword) {
        ResultData<List<HashMap<String, Object>>> data = baseElasticService.search(idxName, properties.split("[,]"), keyword);
        return data;
    }

    /**
     * @param search 查询实体对象
     */
    @Override
    public ResultData<List<HashMap<String, Object>>> findBySearch(ElasticSearch search) {
        if (StringUtils.isEmpty(search.getIdxName())) {
            LOG.warn("索引为空");
            return ResultData.fail("索引为空，不允许提交");
        }
        return baseElasticService.search(search);
    }

    /**
     * @param search 查询实体对象
     */
    @Override
    public ResultData<PageResult<HashMap<String, Object>>> findByPage(ElasticSearch search) {
        if (StringUtils.isEmpty(search.getIdxName())) {
            LOG.warn("索引为空");
            return ResultData.fail("索引为空，不允许提交");
        }
        return baseElasticService.findByPage(search);
    }
}
