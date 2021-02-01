package com.changhong.sei.search.api;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.search.dto.IndexDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 实现功能：ElasticSearch索引的基本管理，提供对外查询、删除和新增功能
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-21 23:35
 */
@Validated
@RequestMapping(path = "elastic", produces = MediaType.APPLICATION_JSON_VALUE)
public interface ElasticIndexApi {
    /**
     * 创建Elastic索引
     */
    @PostMapping("createIndex")
    @ApiOperation(value = "创建Elastic索引", notes = "创建Elastic索引")
    ResultData<String> createIndex(@RequestBody IndexDto indexDto);

    /**
     * 判断索引是否存在；存在-TRUE，否则-FALSE
     */
    @GetMapping("exist/{index}")
    @ApiOperation(value = "判断索引是否存在；存在-TRUE，否则-FALSE", notes = "判断索引是否存在；存在-TRUE，否则-FALSE")
    ResultData<Boolean> indexExist(@PathVariable(value = "index") String index);

    /**
     * 删除索引
     */
    @GetMapping("del/{index}")
    @ApiOperation(value = "删除索引", notes = "删除索引")
    ResultData<Boolean> indexDel(@PathVariable(value = "index") String index);
}
