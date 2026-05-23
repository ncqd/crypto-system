package com.project.crypto.dto;

import java.math.BigDecimal;
import java.util.List;

public record WalletBalanceResponse(
        Long userId,
        List<AssetBalance> balances
) {
    public record AssetBalance(String asset, BigDecimal balance) {}
}
