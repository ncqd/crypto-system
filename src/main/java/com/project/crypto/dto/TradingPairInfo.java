package com.project.crypto.dto;

public record TradingPairInfo(
        String symbol,
        String baseAsset,
        String quoteAsset,
        String description) {}
