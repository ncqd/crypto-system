package com.project.crypto.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.crypto.client.ExchangePriceClient.ExchangeQuote;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class QuoteValidatorTest {

    @Test
    void isUsable_rejectsCrossedSpread() {
        assertThat(QuoteValidator.isUsable(new ExchangeQuote(new BigDecimal("2"), new BigDecimal("1"))))
                .isFalse();
    }

    @Test
    void isUsable_acceptsNormalQuote() {
        assertThat(QuoteValidator.isUsable(new ExchangeQuote(new BigDecimal("1"), new BigDecimal("2"))))
                .isTrue();
    }

    @Test
    void hasPositivePrices_allowsCrossExchangeAggregate() {
        assertThat(QuoteValidator.hasPositivePrices(new BigDecimal("2010"), new BigDecimal("2000")))
                .isTrue();
    }
}
