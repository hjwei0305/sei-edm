package com.changhong.sei.edm.file.service.mongo;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-10 11:30
 */
public class SeiGridFsTemplate extends GridFsTemplate implements SeiGridFsOperations {

    static final String CONTENT_TYPE_FIELD = "_contentType";

    private final MongoDbFactory dbFactory;
    private final MongoConverter converter;
    private final String bucket;

    /**
     * Creates a new {@link GridFsTemplate} using the given {@link MongoDbFactory} and {@link MongoConverter}.
     *
     * @param dbFactory must not be {@literal null}.
     * @param converter must not be {@literal null}.
     * @param bucket
     */
    public SeiGridFsTemplate(MongoDbFactory dbFactory, MongoConverter converter, String bucket) {
        super(dbFactory, converter, bucket);

        this.dbFactory = dbFactory;
        this.converter = converter;
        this.bucket = bucket;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.mongodb.gridfs.GridFsOperations#store(java.io.InputStream, java.lang.String, com.mongodb.Document)
     */
    @Override
    public void store(ObjectId objectId, InputStream content, String filename, String contentType, Object metadata) {
        Assert.notNull(content, "InputStream must not be null!");
        Assert.notNull(filename, "filename must not be null!");

        Document mData = new Document();
        if (StringUtils.hasText(contentType)) {
            mData.put(CONTENT_TYPE_FIELD, contentType);
        }
        if (metadata != null) {
            converter.write(metadata, mData);
        }

        GridFSUploadOptions options = new GridFSUploadOptions();
        options.metadata(mData);

        getGridFs().uploadFromStream(new BsonObjectId(objectId), filename, content, options);
    }

    private GridFSBucket getGridFs() {
        MongoDatabase db = dbFactory.getDb();
        return bucket == null ? GridFSBuckets.create(db) : GridFSBuckets.create(db, bucket);
    }
}
