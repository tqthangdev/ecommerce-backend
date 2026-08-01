package com.dev.ecommerce.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret = "your-256-bit-secret-key-for-dev-only-change-in-prod!!";
    private long accessTokenExpirationMs = 900_000;
    private long refreshTokenExpirationMs = 604_800_000;
}