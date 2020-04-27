package com.changhong.sei.edm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-21 16:13
 */
@ConfigurationProperties("sei.edm")
public class EdmConfigProperties {

    private StoreModel model = StoreModel.local;
    private String storePath;

    private OcrProperties ocr = new OcrProperties();
    private MinioProperties minio = new MinioProperties();

    @NestedConfigurationProperty
    private JodConverterProperties jodConverter = new JodConverterProperties();

    public StoreModel getModel() {
        return model;
    }

    public void setModel(StoreModel model) {
        this.model = model;
    }

    public String getStorePath() {
        return storePath;
    }

    public void setStorePath(String storePath) {
        this.storePath = storePath;
    }

    public OcrProperties getOcr() {
        return ocr;
    }

    public void setOcr(OcrProperties ocr) {
        this.ocr = ocr;
    }

    public MinioProperties getMinio() {
        return minio;
    }

    public void setMinio(MinioProperties minio) {
        this.minio = minio;
    }

    public JodConverterProperties getJodConverter() {
        return jodConverter;
    }

    public void setJodConverter(JodConverterProperties jodConverter) {
        this.jodConverter = jodConverter;
    }

    public enum StoreModel {
        /**
         * 本地文件系统存储管理
         */
        local,
        /**
         * MongoDB存储管理
         */
        mongo,
        /**
         * MinIO是在Apache License v2.0下发布的对象存储服务器
         */
        minio
    }

    /**
     * ocr识别相关配置
     */
    private static class OcrProperties {
        private String matchPrefix;
        private String tessdataPath;

        public String getMatchPrefix() {
            return matchPrefix;
        }

        public void setMatchPrefix(String matchPrefix) {
            this.matchPrefix = matchPrefix;
        }

        public String getTessdataPath() {
            return tessdataPath;
        }

        public void setTessdataPath(String tessdataPath) {
            this.tessdataPath = tessdataPath;
        }
    }

    /**
     * MinIO 配置
     */
    public static class MinioProperties {
        private String endpoint;
        private String accesskey;
        private String secretKey;
        private String bucket;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccesskey() {
            return accesskey;
        }

        public void setAccesskey(String accesskey) {
            this.accesskey = accesskey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
    }
}
