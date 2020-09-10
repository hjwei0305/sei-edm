package com.changhong.sei.edm.file.service.mongo;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.lang.Nullable;

import java.io.InputStream;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-10 11:28
 */
public interface SeiGridFsOperations extends GridFsOperations {
    /**
     * Stores the given content into a file with the given name and content type using the given metadata. The metadata
     * object will be marshalled before writing.
     *
     * @param objectId    must not be {@literal null}.the {@link ObjectId} of the {@link com.mongodb.client.gridfs.model.GridFSFile} just created.
     * @param content     must not be {@literal null}.
     * @param filename    must not be {@literal null} or empty.
     * @param contentType can be {@literal null}.
     * @param metadata    can be {@literal null}
     */
    void store(ObjectId objectId, InputStream content, @Nullable String filename, @Nullable String contentType, @Nullable Object metadata);
}
