package com.project.crypto.service;

import com.project.crypto.client.ExchangePriceClient.ExchangeQuote;
import com.project.crypto.client.ExchangePriceProvider;
import com.project.crypto.domain.entity.AggregatedPrice;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.repository.AggregatedPriceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceAggregationService {

    private static final Logger log = LoggerFactory.getLogger(PriceAggregationService.class);

    private final ExchangePriceProvider exchangePriceProvider;
    private final AggregatedPriceRepository aggregatedPriceRepository;

    public PriceAggregationService(
            ExchangePriceProvider exchangePriceProvider,
            AggregatedPriceRepository aggregatedPriceRepository) {
        this.exchangePriceProvider = exchangePriceProvider;
        this.aggregatedPriceRepository = aggregatedPriceRepository;
    }

    @Transactional
    public void aggregateAndStorePrices() {
        Map<TradingPair, ExchangeQuote> binanceQuotes;
        Map<TradingPair, ExchangeQuote> huobiQuotes;

        try {
            binanceQuotes = exchangePriceProvider.fetchBinanceQuotes();
        } catch (Exception ex) {
            log.warn("Failed to fetch Binance prices: {}", ex.getMessage());
            binanceQuotes = Map.of();
        }

        try {
            huobiQuotes = exchangePriceProvider.fetchHuobiQuotes();
        } catch (Exception ex) {
            log.warn("Failed to fetch Huobi prices: {}", ex.getMessage());
            huobiQuotes = Map.of();
        }

        for (TradingPair pair : TradingPair.values()) {
            ExchangeQuote binance = binanceQuotes.get(pair);
            ExchangeQuote huobi = huobiQuotes.get(pair);

            if (binance == null && huobi == null) {
                log.warn("No quotes available for {}", pair);
                continue;
            }

            BigDecimal bestBid = bestBid(binance, huobi);
            BigDecimal bestAsk = bestAsk(binance, huobi);

            AggregatedPrice price = aggregatedPriceRepository.findBySymbol(pair)
                    .orElseGet(() -> {
                        AggregatedPrice entity = new AggregatedPrice();
                        entity.setSymbol(pair);
                        return entity;
                    });

            price.setBestBidPrice(bestBid);
            price.setBestAskPrice(bestAsk);
            price.setBinanceBidPrice(binance != null ? binance.bidPrice() : null);
            price.setBinanceAskPrice(binance != null ? binance.askPrice() : null);
            price.setHuobiBidPrice(huobi != null ? huobi.bidPrice() : null);
            price.setHuobiAskPrice(huobi != null ? huobi.askPrice() : null);
            price.setUpdatedAt(Instant.now());

            aggregatedPriceRepository.save(price);
            log.debug("Updated aggregated price for {}: bid={}, ask={}", pair, bestBid, bestAsk);
        }
    }

    /**
     * Best bid = highest bid across exchanges (used for SELL orders).
     */
    private BigDecimal bestBid(ExchangeQuote binance, ExchangeQuote huobi) {
        BigDecimal bid = null;
        if (binance != null) {
            bid = binance.bidPrice();
        }
        if (huobi != null) {
            bid = bid == null ? huobi.bidPrice() : bid.max(huobi.bidPrice());
        }
        return bid;
    }

    /**
     * Best ask = lowest ask across exchanges (used for BUY orders).
     */
    private BigDecimal bestAsk(ExchangeQuote binance, ExchangeQuote huobi) {
        BigDecimal ask = null;
        if (binance != null) {
            ask = binance.askPrice();
        }
        if (huobi != null) {
            ask = ask == null ? huobi.askPrice() : ask.min(huobi.askPrice());
        }
        return ask;
    }
}
