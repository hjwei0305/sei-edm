package com.changhong.sei.search.service;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.dto.serach.PageResult;
import com.changhong.sei.core.util.JsonUtils;
import com.changhong.sei.search.dto.ElasticDataDto;
import com.changhong.sei.search.dto.ElasticSearch;
import com.changhong.sei.search.dto.IndexDto;
import com.changhong.sei.search.entity.ElasticEntity;
import com.changhong.sei.util.IdGenerator;
import com.changhong.sei.util.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private static final String INDEX_NAME = "test";
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
        indexDto.setIdxName(INDEX_NAME);
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
        long id = IdGenerator.nextId();
        Map<String, Object> data = new HashMap<>();
        data.put("code", "测试code" + RandomUtils.getString(5));
        data.put("name", "测试name" + RandomUtils.getString(5));
        data.put("url", "测试url" + RandomUtils.getString(5));

        ElasticDataDto elasticDataDto = new ElasticDataDto();
        elasticDataDto.setIdxName(INDEX_NAME);
        elasticDataDto.setId(String.valueOf(id));

        elasticDataDto.setData(data);

        service.save(elasticDataDto.getIdxName(),
                new ElasticEntity(elasticDataDto.getId(), elasticDataDto.getData()));
    }

    @Test
    public void deleteOne() {
        ElasticEntity entity = new ElasticEntity("1371642668253228");
        System.out.println(service.deleteOne(INDEX_NAME, entity));
    }

    @Test
    public void insertBatch() {
        List<ElasticEntity> entities = new ArrayList<>();

        Map<String, Object> data = new HashMap<>();
        data.put("code", "测试code" + RandomUtils.getString(3));
        data.put("name", "测试name" + RandomUtils.getString(5));
        data.put("url", "测试url" + RandomUtils.getString(6));
        entities.add(new ElasticEntity(String.valueOf(IdGenerator.nextId()), data));
        for (int i = 0; i < 100; i++) {
            data = new HashMap<>();
            data.put("code", "测试code" + RandomUtils.getString(i + 5));
            data.put("name", "测试name" + RandomUtils.getString(i + 7));
            data.put("url", "测试url" + RandomUtils.getString(i + 3));
            entities.add(new ElasticEntity(String.valueOf(IdGenerator.nextId()), data));
        }
        service.batchSave(INDEX_NAME, entities);
    }

    @Test
    public void insertBatchTrueObj() {
        List<ElasticEntity> entities = new ArrayList<>();

        Map<String, Object> data = new HashMap<>();
        data.put("code", "测试code" + RandomUtils.getString(3));
        data.put("name", "测试name" + RandomUtils.getString(5));
        data.put("url", "测试url" + RandomUtils.getString(6));
        entities.add(new ElasticEntity(String.valueOf(IdGenerator.nextId()), data));

        data = new HashMap<>();
        data.put("code", "测试code" + RandomUtils.getString(5));
        data.put("name", "测试name" + RandomUtils.getString(7));
        data.put("url", "测试url" + RandomUtils.getString(3));
        entities.add(new ElasticEntity(String.valueOf(IdGenerator.nextId()), data));
        service.batchSaveObj(INDEX_NAME, entities);
    }

    @Test
    public void deleteBatch() {
    }

    @Test
    public void search() {
        ResultData<List<HashMap<String, Object>>> resultData = service.search(INDEX_NAME, "code".split("[,]"), "测试codejlK");
        System.out.println(resultData);
    }

    @Test
    public void findByPage() {
        ElasticSearch search = new ElasticSearch();
        search.setIdxName(INDEX_NAME);
        search.addQuickSearchProperty("code");
        search.addQuickSearchProperty("name");
        search.setQuickSearchValue("mt");
        // 设置高亮字段
        search.setHighlightFields(new String[]{"code", "name"});
        ResultData<PageResult<HashMap<String, Object>>> resultData = service.findByPage(search);
        System.out.println(resultData);
    }

    @Test
    public void deleteIndex() {
    }

    @Test
    public void deleteByQuery() {
    }
}