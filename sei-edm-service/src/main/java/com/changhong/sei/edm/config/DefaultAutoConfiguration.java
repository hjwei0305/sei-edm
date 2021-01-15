package com.changhong.sei.edm.config;

import com.changhong.sei.edm.file.service.FileService;
import com.changhong.sei.edm.file.service.local.LocalFileService;
import com.changhong.sei.edm.file.service.mongo.MongoFileService;
import com.changhong.sei.edm.file.service.mongo.SeiGridFsOperations;
import com.changhong.sei.edm.file.service.mongo.SeiGridFsTemplate;
import com.changhong.sei.edm.file.service.s3.MinIOFileService;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-04-27 10:40
 */
@Configuration
@EnableConfigurationProperties(EdmConfigProperties.class)
public class DefaultAutoConfiguration {

    private final EdmConfigProperties.StoreModel storeModel;
    private final EdmConfigProperties.MinioProperties minioProperties;

    public DefaultAutoConfiguration(final EdmConfigProperties edmConfigProperties) {
        this.storeModel = edmConfigProperties.getModel();
        this.minioProperties = edmConfigProperties.getMinio();
    }

    @Bean
    public FileService fileService() {
        if (EdmConfigProperties.StoreModel.mongo == storeModel) {
            return new MongoFileService();
        } else if (EdmConfigProperties.StoreModel.minio == storeModel) {
            return new MinIOFileService();
        } else {
            return new LocalFileService();
        }
    }

    /**
     * Create a minioClient with the MinIO Server name, Port, Access key and Secret key.
     */
    @Bean
    @ConditionalOnClass(MinioClient.class)
    @ConditionalOnProperty(prefix = "sei.edm", name = "model", havingValue = "minio")
    public MinioClient minioClient() throws InvalidPortException, InvalidEndpointException {
        return new MinioClient(minioProperties.getEndpoint(), minioProperties.getAccesskey(), minioProperties.getSecretKey());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "sei.edm", name = "model", havingValue = "mongo")
    public SeiGridFsOperations seiGridFsTemplate(MongoDatabaseFactory mongoDbFactory, MongoTemplate mongoTemplate) {
        return new SeiGridFsTemplate(mongoDbFactory, mongoTemplate.getConverter(), "fs");
    }

}
