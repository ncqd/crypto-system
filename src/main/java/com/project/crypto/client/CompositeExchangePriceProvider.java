package com.project.crypto.client;

import com.project.crypto.client.ExchangePriceClient.ExchangeQuote;
import com.project.crypto.config.CryptoProperties;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.support.AppLog;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class CompositeExchangePriceProvider implements ExchangePriceProvider {

    private static final Logger log = LoggerFactory.getLogger(CompositeExchangePriceProvider.class);

    private final Optional<ExchangePriceClient> exchangePriceClient;
    private final InternalExchangePriceProvider internalExchangePriceProvider;
    private final CryptoProperties cryptoProperties;

    public CompositeExchangePriceProvider(
            Optional<ExchangePriceClient> exchangePriceClient,
            InternalExchangePriceProvider internalExchangePriceProvider,
            CryptoProperties cryptoProperties) {
        this.exchangePriceClient = exchangePriceClient;
        this.internalExchangePriceProvider = internalExchangePriceProvider;
        this.cryptoProperties = cryptoProperties;
    }

    @Override
    public Map<TradingPair, ExchangeQuote> fetchBinanceQuotes() {
        return fetchQuotes("Binance", () -> exchangePriceClient.get().fetchBinanceQuotes());
    }

    @Override
    public Map<TradingPair, ExchangeQuote> fetchHuobiQuotes() {
        return fetchQuotes("Huobi", () -> exchangePriceClient.get().fetchHuobiQuotes());
    }

    private Map<TradingPair, ExchangeQuote> fetchQuotes(String source, Supplier<Map<TradingPair, ExchangeQuote>> external) {
        if (cryptoProperties.isUseExternalFeeds() && exchangePriceClient.isPresent()) {
            try {
                Map<TradingPair, ExchangeQuote> quotes = external.get();
                if (!quotes.isEmpty()) {
                    AppLog.debug(
                            log,
                            CompositeExchangePriceProvider.class,
                            "fetchQuotes",
                            "%s live quotes symbols=%s".formatted(source, quotes.keySet()));
                    return quotes;
                }
                AppLog.warn(
                        log,
                        CompositeExchangePriceProvider.class,
                        "fetchQuotes",
                        "%s returned no quotes, using internal fallback".formatted(source));
            } catch (Exception ex) {
                AppLog.warn(
                        log,
                        CompositeExchangePriceProvider.class,
                        "fetchQuotes",
                        "%s fetch failed reason=%s, using internal fallback".formatted(source, ex.getMessage()));
            }
        }
        return source.equals("Binance")
                ? internalExchangePriceProvider.fetchBinanceQuotes()
                : internalExchangePriceProvider.fetchHuobiQuotes();
    }
}
