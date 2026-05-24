package com.project.crypto.support;

import com.project.crypto.client.ExchangePriceClient.ExchangeQuote;
import java.math.BigDecimal;

public final class QuoteValidator {

    private QuoteValidator() {}

    public static boolean isUsable(ExchangeQuote quote) {
        return quote != null && isUsable(quote.bidPrice(), quote.askPrice());
    }

    public static boolean isUsable(BigDecimal bid, BigDecimal ask) {
        return bid != null
                && ask != null
                && bid.signum() > 0
                && ask.signum() > 0
                && bid.compareTo(ask) <= 0;
    }

    public static boolean hasPositivePrices(BigDecimal bid, BigDecimal ask) {
        return bid != null && ask != null && bid.signum() > 0 && ask.signum() > 0;
    }

    public static BigDecimal balanceOrZero(BigDecimal balance) {
        return balance != null ? balance : BigDecimal.ZERO;
    }
}
