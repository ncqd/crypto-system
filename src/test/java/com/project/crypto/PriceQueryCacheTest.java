package com.project.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.crypto.config.CacheConfig;
import com.project.crypto.domain.entity.AggregatedPrice;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.repository.AggregatedPriceRepository;
import com.project.crypto.service.PriceQueryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class PriceQueryCacheTest {

    @Autowired
    private PriceQueryService priceQueryService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private AggregatedPriceRepository aggregatedPriceRepository;

    @Test
    void getLatestPrice_usesCacheUntilEvicted() {
        AggregatedPrice eth = new AggregatedPrice();
        eth.setSymbol(TradingPair.ETHUSDT);
        eth.setBestBidPrice(new BigDecimal("2000"));
        eth.setBestAskPrice(new BigDecimal("2001"));
        eth.setUpdatedAt(Instant.now());

        when(aggregatedPriceRepository.findBySymbol(TradingPair.ETHUSDT)).thenReturn(Optional.of(eth));

        priceQueryService.getLatestPrice(TradingPair.ETHUSDT);
        priceQueryService.getLatestPrice(TradingPair.ETHUSDT);

        verify(aggregatedPriceRepository, times(1)).findBySymbol(TradingPair.ETHUSDT);
        assertThat(cacheManager.getCache(CacheConfig.PRICES)).isNotNull();
    }

    @Test
    void getLatestPrices_usesCache() {
        when(aggregatedPriceRepository.findAll()).thenReturn(List.of());

        priceQueryService.getLatestPrices();
        priceQueryService.getLatestPrices();

        verify(aggregatedPriceRepository, times(1)).findAll();
    }
}
