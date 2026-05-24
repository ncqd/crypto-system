package com.project.crypto.dto;

import com.project.crypto.domain.enums.OrderSide;
import com.project.crypto.domain.enums.OrderStatus;
import com.project.crypto.domain.enums.TradingPair;
import java.math.BigDecimal;
import java.time.Instant;

public record LimitOrderResponse(
        Long id,
        TradingPair symbol,
        OrderSide side,
        OrderStatus status,
        BigDecimal quantity,
        BigDecimal limitPrice,
        BigDecimal executionPrice,
        BigDecimal quoteAmount,
        Instant createdAt,
        Instant filledAt,
        Instant cancelledAt
) {}
