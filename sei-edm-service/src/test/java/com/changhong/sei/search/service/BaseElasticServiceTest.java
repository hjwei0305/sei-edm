package com.changhong.sei.search.service;

import com.changhong.sei.core.util.JsonUtils;
import com.changhong.sei.search.dto.ElasticDataDto;
import com.changhong.sei.search.dto.IndexDto;
import com.changhong.sei.search.entity.ElasticEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 01:35
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class BaseElasticServiceTest {
    @Autowired
    private BaseElasticService service;

    @Test
    public void createIndex() {
        Map<String, Map<String, Object>> properties = new HashMap<>();

        Map<String, Object> field = new HashMap<>();
        field.put("type", "long");
        properties.put("id", field);

        field = new HashMap<>();
        field.put("type", "text");
        field.put("index", Boolean.TRUE);
        properties.put("code", field);

        field = new HashMap<>();
        field.put("type", "text");
        field.put("index", Boolean.TRUE);
        field.put("analyzer", "ik_max_word");
        properties.put("name", field);

        field = new HashMap<>();
        field.put("type", "text");
        field.put("index", Boolean.TRUE);
        properties.put("url", field);

        IndexDto.IdxSql idxSql = new IndexDto.IdxSql();
        idxSql.setProperties(properties);

        IndexDto indexDto = new IndexDto();
        indexDto.setIdxName("test");
        indexDto.setIdxSql(idxSql);
        service.createIndex(indexDto.getIdxName(), JsonUtils.toJson(indexDto.getIdxSql()));
    }

    @Test
    public void indexExist() {
    }

    @Test
    public void isExistsIndex() {
    }

    @Test
    public void insertOrUpdateOne() {
        Map<String, Object> data = new HashMap<>();
        data.put("code", "测试code2");
        data.put("name", "测试name2");
        data.put("url", "测试url2");
//        data.put("id", "123");

        ElasticDataDto elasticDataDto = new ElasticDataDto();
        elasticDataDto.setIdxName("test");
//        elasticDataDto.setId("123");

        elasticDataDto.setData(data);

        service.insertOrUpdateOne(elasticDataDto.getIdxName(),
                new ElasticEntity(elasticDataDto.getId(), elasticDataDto.getData()));
    }

    @Test
    public void deleteOne() {
    }

    @Test
    public void insertBatch() {
    }

    @Test
    public void insertBatchTrueObj() {
    }

    @Test
    public void deleteBatch() {
    }

    @Test
    public void search() {
    }

    @Test
    public void deleteIndex() {
    }

    @Test
    public void deleteByQuery() {
    }
}