package com.changhong.sei.search.controller;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.util.JsonUtils;
import com.changhong.sei.search.api.ElasticIndexApi;
import com.changhong.sei.search.dto.IndexDto;
import com.changhong.sei.search.service.BaseElasticService;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 00:27
 */
@RestController
@Api(value = "ElasticIndexApi", tags = "ElasticSearch索引的基本管理，提供对外查询、删除和新增功能")
public class ElasticIndexController implements ElasticIndexApi {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticIndexController.class);

    @Autowired
    private BaseElasticService baseElasticService;

    /**
     * 创建Elastic索引
     */
    @Override
    public ResultData<String> createIndex(IndexDto indexDto) {
        String errMsg;
        try {
            //索引不存在，再创建，否则不允许创建
            if (!baseElasticService.isExistsIndex(indexDto.getIdxName())) {
                String idxSql = JsonUtils.toJson(indexDto.getIdxSql());
                LOG.warn(" idxName={}, idxSql={}", indexDto.getIdxName(), idxSql);
                baseElasticService.createIndex(indexDto.getIdxName(), idxSql);
                return ResultData.success();
            } else {
                errMsg = "索引已经存在，不允许创建";
            }
        } catch (Exception e) {
            errMsg = e.getMessage();
        }
        return ResultData.fail(errMsg);
    }

    /**
     * 判断索引是否存在；存在-TRUE，否则-FALSE
     */
    @Override
    public ResultData<Boolean> indexExist(String index) {
        try {
            boolean existed = baseElasticService.isExistsIndex(index);
            if (!existed) {
                LOG.error("index={},不存在", index);
            } else {
                LOG.info(" 索引已经存在, " + index);
            }
            return ResultData.success(existed);
        } catch (Exception e) {
            LOG.error("调用ElasticSearch 失败！", e);
            return ResultData.fail("调用ElasticSearch 失败！");
        }
    }

    /**
     * 删除索引
     */
    @Override
    public ResultData<Boolean> indexDel(String index) {
        try {
            baseElasticService.deleteIndex(index);
        } catch (Exception e) {
            LOG.error("调用ElasticSearch 失败！", e);
            return ResultData.fail("调用ElasticSearch 失败！");
        }
        return ResultData.success();
    }
}
