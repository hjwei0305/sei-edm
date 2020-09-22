package com.changhong.sei.search.api;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.dto.serach.PageResult;
import com.changhong.sei.search.dto.DocumentElasticDataDto;
import com.changhong.sei.search.dto.ElasticDataDto;
import com.changhong.sei.search.dto.ElasticSearch;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;

/**
 * 实现功能：数据管理
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 00:37
 */
@Validated
@RequestMapping(path = "elasticData", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface ElasticDataApi {

    /**
     * 新增业务文档数据
     */
    @PostMapping(value = "addDoc")
    @ApiOperation(value = "新增业务文档数据", notes = "新增业务文档数据")
    ResultData<String> addDoc(@RequestBody @Valid DocumentElasticDataDto elasticDataDto);

    /**
     * 新增数据
     */
    @PostMapping(value = "add")
    @ApiOperation(value = "新增数据", notes = "新增数据")
    ResultData<String> add(@RequestBody @Valid ElasticDataDto elasticDataDto);

    /**
     * 删除
     */
    @PostMapping(value = "delete")
    @ApiOperation(value = "删除", notes = "删除")
    ResultData<String> delete(@RequestBody @Valid ElasticDataDto elasticDataDto);

    /**
     * @param idxName    索引名
     * @param properties 查询字段.多个用英文逗号分隔
     * @param keyword    查询关键字
     */
    @GetMapping(value = "get/{idxName}")
    @ApiOperation(value = "查询", notes = "查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "properties", value = "查询字段.多个用英文逗号分隔 , ", required = true),
            @ApiImplicitParam(name = "keyword", value = "查询关键字", required = true)
    })
    ResultData<List<HashMap<String, Object>>> findByProperty(@PathVariable("idxName") String idxName, @RequestParam("properties") String properties, @RequestParam("keyword") String keyword);

    /**
     * @param queryDto 查询实体对象
     */
    @PostMapping(value = "findBySearch")
    @ApiOperation(value = "查询实体", notes = "查询实体")
    ResultData<List<HashMap<String, Object>>> findBySearch(@RequestBody @Valid ElasticSearch queryDto);

    /**
     * 分页查询
     */
    @PostMapping(value = "findByPage")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    ResultData<PageResult<HashMap<String, Object>>> findByPage(@RequestBody @Valid ElasticSearch search);
}
