package com.project.crypto.domain.enums;

public enum TradingPair {
    ETHUSDT("ETH", "USDT"),
    BTCUSDT("BTC", "USDT");

    private final String baseAsset;
    private final String quoteAsset;

    TradingPair(String baseAsset, String quoteAsset) {
        this.baseAsset = baseAsset;
        this.quoteAsset = quoteAsset;
    }

    public String getBaseAsset() {
        return baseAsset;
    }

    public String getQuoteAsset() {
        return quoteAsset;
    }

    public String getSymbol() {
        return name();
    }
}
