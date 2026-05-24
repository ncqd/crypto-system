package com.project.crypto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record WalletTransferRequest(
        @NotBlank
        @Pattern(regexp = "USDT|ETH|BTC", message = "asset must be USDT, ETH, or BTC")
        String asset,
        @NotNull
        @DecimalMin(value = "0.00000001", inclusive = false)
        @Digits(integer = 16, fraction = 8)
        BigDecimal amount) {}
