package com.dev.ecommerce.config;

import com.dev.ecommerce.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local")
@RequiredArgsConstructor
public class WebMvcStaticResourceConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String basePath = storageProperties.getLocal().getBasePath();
        String location = basePath.endsWith("/") ? "file:" + basePath : "file:" + basePath + "/";
        String pattern = storageProperties.getLocal().getPublicUrl() + "/**";
        registry.addResourceHandler(pattern).addResourceLocations(location);
    }
}