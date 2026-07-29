package com.dev.ecommerce.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** minio | local | none */
    private String provider = "none";

    private Minio minio = new Minio();
    private Local local = new Local();

    @Getter
    @Setter
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadminpassword";
        private String bucket = "ecommerce-product-images";
        private String publicUrl = "http://localhost:9000";
    }

    @Getter
    @Setter
    public static class Local {
        private String basePath = "./uploads";
        private String publicUrl = "/uploads";
    }
}