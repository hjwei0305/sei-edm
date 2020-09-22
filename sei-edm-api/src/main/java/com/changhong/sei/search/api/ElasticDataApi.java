package com.changhong.sei.search.api;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.search.dto.ElasticDataDto;
import com.changhong.sei.search.dto.QueryDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

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
     * 新增数据
     */
    @PostMapping(value = "add")
    @ApiOperation(value = "新增数据", notes = "新增数据")
    ResultData<String> add(@RequestBody ElasticDataDto elasticDataDto);

    /**
     * 删除
     */
    @PostMapping(value = "/delete")
    @ApiOperation(value = "删除", notes = "删除")
    ResultData<String> delete(@RequestBody ElasticDataDto elasticDataDto);

//    /**
//     * @param index 初始化Location区域，写入数据。
//     */
//    @GetMapping(value = "/addLocation/{index}")
//    @ApiOperation(value = "创建Elastic索引", notes = "创建Elastic索引")
//    ResultData<String> addLocation(@PathVariable(value = "index") String index);

//    void addLocationPage(int pageNum, int pageSize, String index, int lv);

//    void insertDatas(String idxName, List<Location> locations);

    /**
     * @param queryDto 查询实体对象
     */
    @GetMapping(value = "/get")
    @ApiOperation(value = "查询实体", notes = "查询实体")
    ResultData<List<?>> get(@RequestBody QueryDto queryDto);
}
