package com.changhong.sei.edm.file.service.s3;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.file.service.BaseFileService;
import com.changhong.sei.edm.file.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

/**
 * 实现功能：MinIO是在Apache License v2.0下发布的对象存储服务器
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-04-24 09:28
 */
public class MinIOFileService extends BaseFileService implements FileService {

    @Autowired
    private MinioClient minioClient;
    @Value("${sei.edm.minio.bucket:sei-edm}")
    private String bucketName;

    private void inStream2OutStream(InputStream input, OutputStream output) {
        if (input == null) {
            return;
        }
        if (output == null) {
            return;
        }
        try {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > -1) {
                output.write(buffer, 0, len);
            }
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取文档
     *
     * @param docId 文档id
     */
    @Override
    public void getDocByteArray(String docId, OutputStream out) {
        try (InputStream imageStream = minioClient.getObject(bucketName, docId)) {
            inStream2OutStream(imageStream, out);
        } catch (Exception e) {
            LogUtil.error("获取缩略图异常.", e);
        }
    }

    /**
     * 删除文件
     *
     * @param docIds 文档id清单
     */
    @Override
    public void deleteDocuments(Collection<String> docIds) {
        try {
            //删除文档数据
            minioClient.removeObjects(bucketName, docIds);
        } catch (Exception e) {
            LogUtil.error("文件删除异常.", e);
        }
    }

    /**
     * 上传一个文档
     */
    @Override
    public ResultData<Void> storeDocument(String objectId, InputStream inputStream, String fileName, String fileMd5, long size) {
        try {
            // Check if the bucket already exists.
            boolean isExist = minioClient.bucketExists(bucketName);
            if (!isExist) {
                // Make a new bucket called asiatrip to hold a zip file of photos.
                minioClient.makeBucket(bucketName);
            }

            // Upload file to the bucket with putObject
            minioClient.putObject(bucketName, String.valueOf(objectId), inputStream, new PutObjectOptions(inputStream.available(), -1));
            return ResultData.success();
        } catch (Exception e) {
            LogUtil.error("[" + objectId + "]文件上传读取异常.", e);
            return ResultData.fail("[" + objectId + "]文件上传读取异常.");
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
