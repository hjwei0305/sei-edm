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
        mongo
    }
}
