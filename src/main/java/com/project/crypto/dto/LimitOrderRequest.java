package com.project.crypto.dto;

import com.project.crypto.domain.enums.OrderSide;
import com.project.crypto.domain.enums.TradingPair;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LimitOrderRequest(
        @NotNull TradingPair symbol,
        @NotNull OrderSide side,
        @NotNull
        @DecimalMin(value = "0.00000001", inclusive = false)
        @Digits(integer = 16, fraction = 8)
        BigDecimal quantity,
        @NotNull
        @DecimalMin(value = "0.00000001", inclusive = false)
        @Digits(integer = 16, fraction = 8)
        BigDecimal limitPrice
) {}
