package com.project.crypto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crypto.price-aggregation")
public class CryptoProperties {

    private boolean useExternalFeeds = true;
    private long intervalMs = 2000;
    private long maxPriceAgeMs = 30_000;
    private long cacheTtlMs = 2000;
    private String binanceUrl;
    private String huobiUrl;

    public boolean isUseExternalFeeds() {
        return useExternalFeeds;
    }

    public void setUseExternalFeeds(boolean useExternalFeeds) {
        this.useExternalFeeds = useExternalFeeds;
    }

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
