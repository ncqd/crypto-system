package com.project.crypto.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crypto.wallet")
public class WalletProperties {

    private BigDecimal initialUsdtBalance = new BigDecimal("50000.00000000");

    public BigDecimal getInitialUsdtBalance() {
        return initialUsdtBalance;
    }

    public void setInitialUsdtBalance(BigDecimal initialUsdtBalance) {
        this.initialUsdtBalance = initialUsdtBalance;
    }
}
