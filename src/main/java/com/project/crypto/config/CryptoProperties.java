package com.project.crypto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crypto.price-aggregation")
public class CryptoProperties {

    private long intervalMs = 10000;
    private long maxPriceAgeMs = 120_000;
    private long cacheTtlMs = 10_000;
    private String binanceUrl;
    private String huobiUrl;

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public long getMaxPriceAgeMs() {
        return maxPriceAgeMs;
    }

    public void setMaxPriceAgeMs(long maxPriceAgeMs) {
        this.maxPriceAgeMs = maxPriceAgeMs;
    }

    public long getCacheTtlMs() {
        return cacheTtlMs;
    }

    public void setCacheTtlMs(long cacheTtlMs) {
        this.cacheTtlMs = cacheTtlMs;
    }

    public String getBinanceUrl() {
        return binanceUrl;
    }

    public void setBinanceUrl(String binanceUrl) {
        this.binanceUrl = binanceUrl;
    }

    public String getHuobiUrl() {
        return huobiUrl;
    }

    public void setHuobiUrl(String huobiUrl) {
        this.huobiUrl = huobiUrl;
    }
}
