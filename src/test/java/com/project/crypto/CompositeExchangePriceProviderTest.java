package com.project.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.project.crypto.client.CompositeExchangePriceProvider;
import com.project.crypto.client.ExchangePriceClient;
import com.project.crypto.client.ExchangePriceClient.ExchangeQuote;
import com.project.crypto.client.InternalExchangePriceProvider;
import com.project.crypto.config.CryptoProperties;
import com.project.crypto.domain.enums.TradingPair;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompositeExchangePriceProviderTest {

    @Mock
    private ExchangePriceClient exchangePriceClient;

    @Mock
    private InternalExchangePriceProvider internalExchangePriceProvider;

    private CryptoProperties cryptoProperties;

    @BeforeEach
    void setUp() {
        cryptoProperties = new CryptoProperties();
    }

    @Test
    void uses_external_when_enabled() {
        cryptoProperties.setUseExternalFeeds(true);
        var provider = new CompositeExchangePriceProvider(
                Optional.of(exchangePriceClient), internalExchangePriceProvider, cryptoProperties);

        when(exchangePriceClient.fetchBinanceQuotes())
                .thenReturn(Map.of(
                        TradingPair.ETHUSDT,
                        new ExchangeQuote(new BigDecimal("3000"), new BigDecimal("3001"))));

        var quotes = provider.fetchBinanceQuotes();

        assertThat(quotes.get(TradingPair.ETHUSDT).bidPrice()).isEqualByComparingTo("3000");
    }

    @Test
    void falls_back_to_internal_when_external_empty() {
        cryptoProperties.setUseExternalFeeds(true);
        var provider = new CompositeExchangePriceProvider(
                Optional.of(exchangePriceClient), internalExchangePriceProvider, cryptoProperties);

        when(exchangePriceClient.fetchBinanceQuotes()).thenReturn(Map.of());
        when(internalExchangePriceProvider.fetchBinanceQuotes())
                .thenReturn(Map.of(
                        TradingPair.ETHUSDT,
                        new ExchangeQuote(new BigDecimal("2000"), new BigDecimal("2001"))));

        var quotes = provider.fetchBinanceQuotes();

        assertThat(quotes.get(TradingPair.ETHUSDT).askPrice()).isEqualByComparingTo("2001");
    }

    @Test
    void uses_internal_when_external_disabled() {
        cryptoProperties.setUseExternalFeeds(false);
        var provider = new CompositeExchangePriceProvider(
                Optional.empty(), internalExchangePriceProvider, cryptoProperties);

        when(internalExchangePriceProvider.fetchHuobiQuotes())
                .thenReturn(Map.of(
                        TradingPair.BTCUSDT,
                        new ExchangeQuote(new BigDecimal("70000"), new BigDecimal("70010"))));

        var quotes = provider.fetchHuobiQuotes();

        assertThat(quotes).containsKey(TradingPair.BTCUSDT);
    }
}
