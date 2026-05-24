package com.project.crypto.service;

import com.project.crypto.client.ExchangePriceClient.ExchangeQuote;
import com.project.crypto.client.ExchangePriceProvider;
import com.project.crypto.config.CacheConfig;
import com.project.crypto.domain.entity.AggregatedPrice;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.repository.AggregatedPriceRepository;
import com.project.crypto.support.AppLog;
import com.project.crypto.support.QuoteValidator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
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

    @CacheEvict(cacheNames = CacheConfig.PRICES, allEntries = true)
    @Transactional
    public void updatePrices() {
        Map<TradingPair, ExchangeQuote> binanceQuotes;
        Map<TradingPair, ExchangeQuote> huobiQuotes;

        try {
            binanceQuotes = exchangePriceProvider.fetchBinanceQuotes();
        } catch (Exception ex) {
            AppLog.warn(
                    log,
                    PriceAggregationService.class,
                    "updatePrices",
                    "BinanceFetch failed reason=%s".formatted(ex.getMessage()));
            binanceQuotes = Map.of();
        }

        try {
            huobiQuotes = exchangePriceProvider.fetchHuobiQuotes();
        } catch (Exception ex) {
            AppLog.warn(
                    log,
                    PriceAggregationService.class,
                    "updatePrices",
                    "HuobiFetch failed reason=%s".formatted(ex.getMessage()));
            huobiQuotes = Map.of();
        }

        for (TradingPair pair : TradingPair.values()) {
            ExchangeQuote binance = usableQuote(binanceQuotes.get(pair));
            ExchangeQuote huobi = usableQuote(huobiQuotes.get(pair));

            if (binance == null && huobi == null) {
                AppLog.warn(
                        log,
                        PriceAggregationService.class,
                        "updatePrices",
                        "AggregatedPrice symbol=%s no quotes".formatted(pair));
                continue;
            }

            BigDecimal bestBid = maxBid(binance, huobi);
            BigDecimal bestAsk = minAsk(binance, huobi);
            if (!QuoteValidator.hasPositivePrices(bestBid, bestAsk)) {
                AppLog.warn(
                        log,
                        PriceAggregationService.class,
                        "updatePrices",
                        "AggregatedPrice symbol=%s skipped invalid bid/ask".formatted(pair));
                continue;
            }

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
            AppLog.info(
                    log,
                    PriceAggregationService.class,
                    "updatePrices",
                    "AggregatedPrice symbol=%s bid=%s ask=%s".formatted(pair, bestBid, bestAsk));
        }
    }

    private ExchangeQuote usableQuote(ExchangeQuote quote) {
        return QuoteValidator.isUsable(quote) ? quote : null;
    }

    private BigDecimal maxBid(ExchangeQuote binance, ExchangeQuote huobi) {
        BigDecimal bid = null;
        if (binance != null) {
            bid = binance.bidPrice();
        }
        if (huobi != null) {
            bid = bid == null ? huobi.bidPrice() : bid.max(huobi.bidPrice());
        }
        return bid;
    }

    private BigDecimal minAsk(ExchangeQuote binance, ExchangeQuote huobi) {
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
