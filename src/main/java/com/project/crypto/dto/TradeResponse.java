package com.project.crypto.dto;

import com.project.crypto.domain.enums.OrderSide;
import com.project.crypto.domain.enums.TradingPair;
import java.math.BigDecimal;
import java.time.Instant;

public record TradeResponse(
        Long id,
        TradingPair symbol,
        OrderSide side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal quoteAmount,
        Instant createdAt
) {}
