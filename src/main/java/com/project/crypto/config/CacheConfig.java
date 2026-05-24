package com.project.crypto.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRICES = "prices";

    @Bean
    public CacheManager cacheManager(CryptoProperties cryptoProperties) {
        CaffeineCacheManager manager = new CaffeineCacheManager(PRICES);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(cryptoProperties.getCacheTtlMs()))
                .maximumSize(16));
        return manager;
    }
}
