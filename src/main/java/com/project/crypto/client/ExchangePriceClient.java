package com.project.crypto.client;

import com.project.crypto.client.dto.BinanceBookTicker;
import com.project.crypto.client.dto.HuobiTickerResponse;
import com.project.crypto.config.CryptoProperties;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.support.QuoteValidator;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "crypto.price-aggregation.use-external-feeds", havingValue = "true")
public class ExchangePriceClient implements ExchangePriceProvider {

    private final RestClient restClient;
    private final CryptoProperties properties;

    public ExchangePriceClient(RestClient restClient, CryptoProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public Map<TradingPair, ExchangeQuote> fetchBinanceQuotes() {
        BinanceBookTicker[] tickers = restClient.get()
                .uri(properties.getBinanceUrl())
                .retrieve()
                .body(BinanceBookTicker[].class);

        if (tickers == null) {
            return Map.of();
        }

        return Arrays.stream(tickers)
                .filter(ticker -> isSupportedSymbol(ticker.getSymbol()))
                .map(ticker -> Map.entry(
                        TradingPair.valueOf(ticker.getSymbol()),
                        toQuote(ticker.getBidPrice(), ticker.getAskPrice())))
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
    }

    @Override
    public Map<TradingPair, ExchangeQuote> fetchHuobiQuotes() {
        HuobiTickerResponse response = restClient.get()
                .uri(properties.getHuobiUrl())
                .retrieve()
                .body(HuobiTickerResponse.class);

        if (response == null || response.getData() == null) {
            return Map.of();
        }

        return response.getData().stream()
                .filter(ticker -> isSupportedHuobiSymbol(ticker.getSymbol()))
                .map(ticker -> Map.entry(
                        toTradingPair(ticker.getSymbol()),
                        toQuote(ticker.getBid(), ticker.getAsk())))
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get(), (a, b) -> b));
    }

    private Optional<ExchangeQuote> toQuote(String bid, String ask) {
        if (bid == null || ask == null || bid.isBlank() || ask.isBlank()) {
            return Optional.empty();
        }
        try {
            BigDecimal bidPrice = new BigDecimal(bid.trim());
            BigDecimal askPrice = new BigDecimal(ask.trim());
            if (!QuoteValidator.isUsable(bidPrice, askPrice)) {
                return Optional.empty();
            }
            return Optional.of(new ExchangeQuote(bidPrice, askPrice));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private Optional<ExchangeQuote> toQuote(double bid, double ask) {
        if (!Double.isFinite(bid) || !Double.isFinite(ask)) {
            return Optional.empty();
        }
        BigDecimal bidPrice = BigDecimal.valueOf(bid);
        BigDecimal askPrice = BigDecimal.valueOf(ask);
        if (!QuoteValidator.isUsable(bidPrice, askPrice)) {
            return Optional.empty();
        }
        return Optional.of(new ExchangeQuote(bidPrice, askPrice));
    }

    private boolean isSupportedSymbol(String symbol) {
        try {
            TradingPair.valueOf(symbol);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isSupportedHuobiSymbol(String symbol) {
        return symbol != null && (symbol.equalsIgnoreCase("ethusdt") || symbol.equalsIgnoreCase("btcusdt"));
    }

    private TradingPair toTradingPair(String symbol) {
        return TradingPair.valueOf(symbol.toUpperCase());
    }

    public record ExchangeQuote(BigDecimal bidPrice, BigDecimal askPrice) {}
}
