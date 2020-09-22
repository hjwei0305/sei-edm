package com.changhong.sei.search.service;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.util.JsonUtils;
import com.changhong.sei.edm.dto.DocumentResponse;
import com.changhong.sei.edm.file.service.FileService;
import com.changhong.sei.search.common.util.ReadFileContentUtil;
import com.changhong.sei.search.dto.DocumentElasticDataDto;
import com.changhong.sei.search.dto.IndexDto;
import com.changhong.sei.search.entity.ElasticEntity;
import com.changhong.sei.util.FileUtils;
import com.changhong.sei.util.IdGenerator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 15:22
 */
@Service
@EnableAsync
public class AsyncDocumentContextService {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncDocumentContextService.class);
    private static final String ELASTIC_FIELD_DOC_ID = "docId";
    private static final String ELASTIC_FIELD_DOC_CONTENT = "docContent";

    @Autowired
    private FileService fileService;
    @Autowired
    private BaseElasticService elasticService;

    /**
     * 异步识别并持久化es文档内容
     */
//    @Async
    public void recognizeAndSaveElastic(DocumentElasticDataDto dataDto) {
        String[] docIds = dataDto.getDocIds();
        if (docIds == null || docIds.length == 0) {
            LOG.error("文档id不能为空.");
            return;
        }
        String idxName = dataDto.getIdxName();
        if (StringUtils.isBlank(idxName)) {
            LOG.error("索引名不能为空.");
            return;
        }
        Set<String> docIdSet = new HashSet<>();
        Collections.addAll(docIdSet, docIds);
        List<DocumentResponse> documents = fileService.getDocumentInfo(docIdSet);
        if (CollectionUtils.isNotEmpty(documents)) {
            try {
                // 检查索引是否存在
                boolean indexExist = elasticService.indexExist(idxName);
                if (!indexExist) {
                    String idxSql = JsonUtils.toJson(buildIndex(dataDto));
                    // 不存在创建所以
                    elasticService.createIndex(idxName, idxSql);
                }
            } catch (Exception e) {
                LOG.error("初始化文档解析异常", e);
            }

            ElasticEntity entity;
            List<ElasticEntity> entities = new ArrayList<>();
            Map<String, Object> data = dataDto.getData();
            for (DocumentResponse document : documents) {
                entity = new ElasticEntity(String.valueOf(IdGenerator.nextId()), data);
                try {
                    entity.addData(ELASTIC_FIELD_DOC_ID, document.getDocId());
                    entity.addData(ELASTIC_FIELD_DOC_CONTENT, this.recognizeDocumentContent(document));
                } catch (Exception e) {
                    LOG.error("读取文档内容异常", e);
                }
//                ResultData<String> resultData = elasticService.save(idxName, entity);
//                LOG.info("异步识别并持久化es文档内容结果: {}", resultData);
                entities.add(entity);
            }

            ResultData<String> resultData = elasticService.batchSave(idxName, entities);
            LOG.info("异步识别并持久化es文档内容结果: {}", resultData);
        }
    }

    private IndexDto.IdxSql buildIndex(DocumentElasticDataDto dto) {
        Set<String> keySet = dto.getData().keySet();

        Map<String, Map<String, Object>> properties = new HashMap<>();
        Map<String, Object> field;
        for (String key : keySet) {
            field = new HashMap<>();
            field.put("type", "text");
            field.put("index", Boolean.TRUE);
            properties.put(key, field);
        }

        // 追加文档内容
        field = new HashMap<>();
        field.put("type", "text");
        field.put("index", Boolean.TRUE);
        field.put("analyzer", "ik_max_word");
        properties.put(ELASTIC_FIELD_DOC_CONTENT, field);
        // 追加docId
        if (!properties.containsKey(ELASTIC_FIELD_DOC_ID)) {
            field = new HashMap<>();
            field.put("type", "text");
            field.put("index", Boolean.TRUE);
            properties.put(ELASTIC_FIELD_DOC_ID, field);
        }

        IndexDto.IdxSql idxSql = new IndexDto.IdxSql();
        idxSql.setProperties(properties);

        return idxSql;
    }

    /**
     * 识别文档内容
     *
     * @param document 文档
     * @return 文档内容
     */
    private String recognizeDocumentContent(DocumentResponse document) {
        if (Objects.isNull(document)) {
            return null;
        }
        String docId = document.getDocId();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        fileService.getDocumentOutputStream(docId, document.getHasChunk(), out);
        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());

        String fileName = document.getFileName();
        String suffix = FileUtils.getExtension(fileName);
        //文件后缀
        StringBuilder content = new StringBuilder(fileName);
        switch (document.getDocumentType()) {
            case Text:
                content.append(ReadFileContentUtil.readTxtContent(inputStream));
                break;
            case Pdf:
                content.append(ReadFileContentUtil.readPdfContent(inputStream));
                break;
            case Word:
                if ("docx".equalsIgnoreCase(suffix)) {
                    content.append(ReadFileContentUtil.readDocxContent(inputStream));
                }
                if ("doc".equalsIgnoreCase(suffix)) {
                    content.append(ReadFileContentUtil.readDocContent(inputStream));
                }
                break;
            case Excel:
                if ("xls".equalsIgnoreCase(suffix)) {
                    content.append(ReadFileContentUtil.readXlsContent(inputStream));
                }
                if ("xlsx".equalsIgnoreCase(suffix)) {
                    content.append(ReadFileContentUtil.readXlsxContent(inputStream));
                }
                break;
            case Powerpoint:
                if ("ppt".equalsIgnoreCase(suffix)) {
                    content.append(ReadFileContentUtil.readPptContent(inputStream));
                }
                if ("pptx".equalsIgnoreCase(suffix)) {
                    content.append(ReadFileContentUtil.readPptxContent(inputStream));
                }
                break;
            default:

        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("文档[{}]识别内容：{}", document.getFileName(), content);
        }
        return content.toString();
    }
}
