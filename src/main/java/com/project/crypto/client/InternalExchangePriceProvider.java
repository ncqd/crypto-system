package com.project.crypto.client;

import com.project.crypto.client.ExchangePriceClient.ExchangeQuote;
import com.project.crypto.config.MarketPriceProperties;
import com.project.crypto.domain.enums.TradingPair;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InternalExchangePriceProvider implements ExchangePriceProvider {

    private final MarketPriceProperties marketPriceProperties;

    @Override
    public Map<TradingPair, ExchangeQuote> fetchBinanceQuotes() {
        return toQuotes(marketPriceProperties.getPrimary());
    }

    @Override
    public Map<TradingPair, ExchangeQuote> fetchHuobiQuotes() {
        return toQuotes(marketPriceProperties.getSecondary());
    }

    private Map<TradingPair, ExchangeQuote> toQuotes(Map<String, MarketPriceProperties.Quote> source) {
        Map<TradingPair, ExchangeQuote> quotes = new EnumMap<>(TradingPair.class);
        for (TradingPair pair : TradingPair.values()) {
            MarketPriceProperties.Quote quote = source.get(pair.name());
            if (quote != null) {
                quotes.put(pair, new ExchangeQuote(quote.getBid(), quote.getAsk()));
            }
        }
        return quotes;
    }
}
