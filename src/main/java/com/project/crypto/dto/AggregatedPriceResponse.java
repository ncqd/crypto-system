package com.project.crypto.dto;

import com.project.crypto.domain.enums.TradingPair;
import java.math.BigDecimal;
import java.time.Instant;

public record AggregatedPriceResponse(
        TradingPair symbol,
        BigDecimal bestBidPrice,
        BigDecimal bestAskPrice,
        BigDecimal binanceBidPrice,
        BigDecimal binanceAskPrice,
        BigDecimal huobiBidPrice,
        BigDecimal huobiAskPrice,
        Instant updatedAt
) {}
