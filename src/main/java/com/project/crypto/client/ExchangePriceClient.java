package com.project.crypto.client;

import com.project.crypto.client.dto.BinanceBookTicker;
import com.project.crypto.client.dto.HuobiTickerResponse;
import com.project.crypto.config.CryptoProperties;
import com.project.crypto.domain.enums.TradingPair;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ExchangePriceClient implements ExchangePriceProvider {

    private final RestClient restClient;
    private final CryptoProperties properties;

    public ExchangePriceClient(RestClient restClient, CryptoProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

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
                .collect(Collectors.toMap(
                        ticker -> TradingPair.valueOf(ticker.getSymbol()),
                        ticker -> new ExchangeQuote(
                                new BigDecimal(ticker.getBidPrice()),
                                new BigDecimal(ticker.getAskPrice())
                        )
                ));
    }

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
                .collect(Collectors.toMap(
                        ticker -> toTradingPair(ticker.getSymbol()),
                        ticker -> new ExchangeQuote(
                                BigDecimal.valueOf(ticker.getBid()),
                                BigDecimal.valueOf(ticker.getAsk())
                        ),
                        (existing, replacement) -> replacement
                ));
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
