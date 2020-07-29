package com.ecmp.edm.manager;

import com.ecmp.edm.entity.Document;
import com.ecmp.edm.entity.DocumentInfo;
import com.ecmp.vo.ResponseData;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：文档管理器接口定义
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017-07-11 14:27      王锦光(wangj)                新建
 * <p/>
 * *************************************************************************************************
 */
public interface IDocumentManager {

    /**
     * 上传一个文档(如果是图像生成缩略图)
     *
     * @param document 文档
     * @return 文档信息
     */
    DocumentInfo uploadDocument(Document document);

    /**
     * 上传一个文档(如果是图像生成缩略图)
     *
     * @param stream   文档数据流
     * @param fileName 文件名
     * @return 文档信息
     */
    DocumentInfo uploadDocument(InputStream stream, String fileName);

    /**
     * 获取一个文档(包含信息和数据)
     *
     * @param id          文档Id
     * @param isThumbnail 是获取缩略图
     * @return 文档
     */
    Document getDocument(String id, boolean isThumbnail);

    /**
     * 获取一个文档(包含信息和数据)
     *
     * @param id 文档Id
     * @return 文档
     */
    Document getDocument(String id);

    /**
     * 提交业务实体的文档信息
     *
     * @param entityId    业务实体Id
     * @param documentIds 文档Id清单
     */
    void submitBusinessInfos(String entityId, Collection<String> documentIds);

    /**
     * 删除业务实体的文档信息
     * @param entityId 业务实体Id
     */
    void deleteBusinessInfos(String entityId);

    /**
     * 获取业务实体的文档信息清单
     *
     * @param entityId 业务实体Id
     * @return 文档信息清单
     */
    List<DocumentInfo> getEntityDocumentInfos(String entityId);

    /**
     * 转为pdf文件并存储
     * 目前支持Word,Powerpoint转为pdf文件
     *
     * @param docId    文档id,必须
     * @param markText 文档水印
     * @return 返回成功转为pdf存储的docId, 不能成功转为pdf的返回原docId
     */
    ResponseData<String> convert2PdfAndSave(String docId, String markText);
}
