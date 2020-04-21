package com.ecmp.edm.config;

import com.ecmp.edm.manager.DocumentManager;
import com.ecmp.edm.manager.IDocumentManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 实现功能：开发工具包配置类
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-04-21 13:49
 */
@Configuration
public class EdmSdkAutoConfig {

    @Bean
    public IDocumentManager documentManager() {
        return new DocumentManager();
    }
}
