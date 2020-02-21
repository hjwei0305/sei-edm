package com.changhong.sei.edm.config;

import com.changhong.sei.edm.file.service.FileService;
import com.changhong.sei.edm.file.service.local.LocalFileServiceImpl;
import com.changhong.sei.edm.file.service.mongo.MongoServiceImpl;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jodconverter.DocumentConverter;
import org.jodconverter.LocalConverter;
import org.jodconverter.document.DefaultDocumentFormatRegistryInstanceHolder;
import org.jodconverter.document.DocumentFormatRegistry;
import org.jodconverter.document.JsonDocumentFormatRegistry;
import org.jodconverter.office.LocalOfficeManager;
import org.jodconverter.office.LocalOfficeUtils;
import org.jodconverter.office.OfficeManager;
import org.jodconverter.process.ProcessManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 实现功能：JodConverter配置类
 * http://www.jodconverter.org
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-02-07 18:32
 */
@Configuration
@ConditionalOnClass(LocalConverter.class)
@ConditionalOnProperty(prefix = "sei.edm.jod-converter", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(EdmConfigProperties.class)
public class JodConverterConfig implements ResourceLoaderAware {

    private final EdmConfigProperties.StoreModel storeModel;
    private final JodConverterProperties properties;
    private ResourceLoader resourceLoader;

    public JodConverterConfig(final EdmConfigProperties edmConfigProperties) {
        this.storeModel = edmConfigProperties.getModel();
        this.properties = edmConfigProperties.getJodConverter();
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public FileService fileService() {
        if (EdmConfigProperties.StoreModel.local == storeModel) {
            return new LocalFileServiceImpl();
        } else {
            return new MongoServiceImpl();
        }
     }

    // Creates the OfficeManager bean.
    private OfficeManager createOfficeManager(final ProcessManager processManager) {
        final LocalOfficeManager.Builder builder = LocalOfficeManager.builder();

        if (!StringUtils.isBlank(properties.getPortNumbers())) {
            builder.portNumbers(
                    ArrayUtils.toPrimitive(
                            Stream.of(StringUtils.split(properties.getPortNumbers(), ", "))
                                    .map(str -> NumberUtils.toInt(str, 2002))
                                    .toArray(Integer[]::new)));
        }

        builder.officeHome(properties.getOfficeHome());
        builder.workingDir(properties.getWorkingDir());
        builder.templateProfileDir(properties.getTemplateProfileDir());
        builder.killExistingProcess(properties.isKillExistingProcess());
        builder.processTimeout(properties.getProcessTimeout());
        builder.processRetryInterval(properties.getProcessRetryInterval());
        builder.taskExecutionTimeout(properties.getTaskExecutionTimeout());
        builder.maxTasksPerProcess(properties.getMaxTasksPerProcess());
        builder.taskQueueTimeout(properties.getTaskQueueTimeout());
        final String processManagerClass = properties.getProcessManagerClass();
        if (StringUtils.isNotEmpty(processManagerClass)) {
            builder.processManager(processManagerClass);
        } else {
            builder.processManager(processManager);
        }

        // Starts the manager
        return builder.build();
    }

    @Bean
    public ProcessManager processManager() {
        return LocalOfficeUtils.findBestProcessManager();
    }

    @Bean
    public DocumentFormatRegistry documentFormatRegistry() throws Exception {
        DocumentFormatRegistry registry;
        if (StringUtils.isBlank(properties.getDocumentFormatRegistry())) {
            try (InputStream in = resourceLoader.getResource("classpath:document-formats.json").getInputStream()) {
                registry = JsonDocumentFormatRegistry.create(in, properties.getFormatOptions());
            }
        } else {
            try (InputStream in = resourceLoader.getResource(properties.getDocumentFormatRegistry()).getInputStream()) {
                registry = JsonDocumentFormatRegistry.create(in, properties.getFormatOptions());
            }
        }

        // Set as default.
        DefaultDocumentFormatRegistryInstanceHolder.setInstance(registry);

        // Return it.
        return registry;
    }

    @Bean(name = "localOfficeManager", initMethod = "start", destroyMethod = "stop")
    public OfficeManager localOfficeManager(final ProcessManager processManager) {
        return createOfficeManager(processManager);
    }

    // Must appear after the localOfficeManager bean creation. Do not reorder this class by name.
    @Bean
    public DocumentConverter localDocumentConverter(
            final OfficeManager localOfficeManager, final DocumentFormatRegistry documentFormatRegistry) {

        Map<String, Object> loadProperties = new HashMap<>(10);
        loadProperties.put("Hidden", true);
        loadProperties.put("ReadOnly", true);
        // com.sun.star.document.UpdateDocMode.QUIET_UPDATE
        loadProperties.put("UpdateDocMode", (short) 1);
        loadProperties.put("CharacterSet", StandardCharsets.UTF_8.name());

        return LocalConverter.builder()
                .officeManager(localOfficeManager)
                .formatRegistry(documentFormatRegistry)
                .loadProperties(loadProperties)
                .build();
    }
}
