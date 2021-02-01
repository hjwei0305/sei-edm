package com.changhong.sei.edm.file.service.mongo;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.common.util.DocumentTypeUtil;
import com.changhong.sei.edm.dto.DocumentType;
import com.changhong.sei.edm.file.service.BaseFileService;
import com.changhong.sei.edm.file.service.FileService;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Objects;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-03 14:08
 */
public class MongoFileService extends BaseFileService implements FileService {

    @Autowired
    private MongoDatabaseFactory mongoDbFactory;
    //    @Autowired
//    private GridFsOperations edmGridFsTemplate;
    @Autowired
    private SeiGridFsOperations seiGridFsTemplate;

    /**
     * 获取文档
     *
     * @param docId 文档id
     */
    @Override
    public void getDocByteArray(String docId, OutputStream out) {
        //获取原图
        GridFSFile fsdbFile = seiGridFsTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(docId)));
        if (Objects.isNull(fsdbFile)) {
            LogUtil.error("[{}]文件不存在.", docId);
            return;
        }
        GridFSBucket bucket = GridFSBuckets.create(mongoDbFactory.getMongoDatabase());
        bucket.downloadToStream(fsdbFile.getId(), out);
    }

    /**
     * 删除文件
     *
     * @param docIds 文档id清单
     */
    @Override
    public void deleteDocuments(Collection<String> docIds) {
        Query query;
        for (String docId : docIds) {
            try {
                //删除文档数据
                query = new Query().addCriteria(Criteria.where("_id").is(docId));
                seiGridFsTemplate.delete(query);
            } catch (Exception e) {
                LogUtil.error("[" + docId + "]文件删除异常.", e);
            }
        }
    }

    /**
     * 上传一个文档
     */
    @Override
    public ResultData<Void> storeDocument(String objectId, InputStream inputStream, String fileName, String fileMd5, long size) {
        try {
            DocumentType documentType = DocumentTypeUtil.getDocumentType(fileName);

            //保存数据文件
            DBObject metaData = new BasicDBObject();
            metaData.put("description", fileName);
//            ObjectId objectId = edmGridFsTemplate.store(inputStream, fileName, documentType.toString(), metaData);
            ObjectId objId = new ObjectId(objectId);
            seiGridFsTemplate.store(objId, inputStream, fileName, documentType.toString(), metaData);
            return ResultData.success();
        } catch (Exception e) {
            LogUtil.error("[" + objectId + "]文件上传读取异常.", e);
            return ResultData.fail("[" + objectId + "]文件上传读取异常.");
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
