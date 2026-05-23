package com.project.crypto;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import com.project.crypto.client.ExchangePriceClient.ExchangeQuote;
import com.project.crypto.client.ExchangePriceProvider;
import com.project.crypto.domain.entity.AggregatedPrice;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.repository.AggregatedPriceRepository;
import com.project.crypto.service.PriceAggregationService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceAggregationServiceTest {

    @Mock
    private ExchangePriceProvider exchangePriceProvider;

    @Mock
    private AggregatedPriceRepository aggregatedPriceRepository;

    private PriceAggregationService priceAggregationService;

    @BeforeEach
    void setUp() {
        priceAggregationService = new PriceAggregationService(exchangePriceProvider, aggregatedPriceRepository);
    }

    @Test
    void aggregateAndStorePrices_picksBestBidAndAskAcrossExchanges() {
        when(exchangePriceProvider.fetchBinanceQuotes()).thenReturn(Map.of(
                TradingPair.ETHUSDT, new ExchangeQuote(new BigDecimal("2000.00"), new BigDecimal("2001.00")),
                TradingPair.BTCUSDT, new ExchangeQuote(new BigDecimal("70000.00"), new BigDecimal("70010.00"))
        ));
        when(exchangePriceProvider.fetchHuobiQuotes()).thenReturn(Map.of(
                TradingPair.ETHUSDT, new ExchangeQuote(new BigDecimal("2000.50"), new BigDecimal("2000.80")),
                TradingPair.BTCUSDT, new ExchangeQuote(new BigDecimal("69990.00"), new BigDecimal("70005.00"))
        ));
        when(aggregatedPriceRepository.findBySymbol(TradingPair.ETHUSDT)).thenReturn(Optional.empty());
        when(aggregatedPriceRepository.findBySymbol(TradingPair.BTCUSDT)).thenReturn(Optional.empty());
        when(aggregatedPriceRepository.save(org.mockito.ArgumentMatchers.any(AggregatedPrice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        priceAggregationService.aggregateAndStorePrices();

        ArgumentCaptor<AggregatedPrice> captor = ArgumentCaptor.forClass(AggregatedPrice.class);
        org.mockito.Mockito.verify(aggregatedPriceRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        AggregatedPrice eth = captor.getAllValues().stream()
                .filter(price -> price.getSymbol() == TradingPair.ETHUSDT)
                .findFirst()
                .orElseThrow();

        assertThat(eth.getBestBidPrice()).isEqualByComparingTo("2000.50");
        assertThat(eth.getBestAskPrice()).isEqualByComparingTo("2000.80");
    }
}
