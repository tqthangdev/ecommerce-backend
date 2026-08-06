package com.dev.ecommerce.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_BRANDS = "brands";
    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_PRODUCT_DETAIL = "product-detail";
    public static final String CACHE_COUPONS = "coupons";
    public static final String CACHE_PROMOTIONS = "promotions";
    public static final String CACHE_PROMOTION_PRICE = "promotion-price";

    @Bean
    CacheManager cacheManager(RedisConnectionFactory factory) {
        ObjectMapper redisObjectMapper = new ObjectMapper();
        redisObjectMapper.registerModule(new JavaTimeModule());
        redisObjectMapper.activateDefaultTyping(
                redisObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        var serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                CACHE_CATEGORIES, defaultConfig.entryTtl(Duration.ofMinutes(30)),
                CACHE_BRANDS, defaultConfig.entryTtl(Duration.ofMinutes(30)),
                CACHE_PRODUCTS, defaultConfig.entryTtl(Duration.ofMinutes(10)),
                CACHE_PRODUCT_DETAIL, defaultConfig.entryTtl(Duration.ofMinutes(5)),
                CACHE_COUPONS, defaultConfig.entryTtl(Duration.ofMinutes(15)),
                CACHE_PROMOTIONS, defaultConfig.entryTtl(Duration.ofMinutes(15)),
                CACHE_PROMOTION_PRICE, defaultConfig.entryTtl(Duration.ofMinutes(5))
        );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}