package com.project.crypto.service;

import com.project.crypto.config.CacheConfig;
import com.project.crypto.domain.entity.AggregatedPrice;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.dto.AggregatedPriceResponse;
import com.project.crypto.exception.ResourceNotFoundException;
import com.project.crypto.repository.AggregatedPriceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PriceQueryService {

    private final AggregatedPriceRepository aggregatedPriceRepository;

    @Cacheable(cacheNames = CacheConfig.PRICES, key = "'all'")
    public List<AggregatedPriceResponse> getLatestPrices() {
        return aggregatedPriceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(cacheNames = CacheConfig.PRICES, key = "#symbol")
    public AggregatedPriceResponse getLatestPrice(TradingPair symbol) {
        AggregatedPrice price = aggregatedPriceRepository.findBySymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No aggregated price available for " + symbol + ". Wait for the scheduler to refresh."));
        return toResponse(price);
    }

    private AggregatedPriceResponse toResponse(AggregatedPrice price) {
        return new AggregatedPriceResponse(
                price.getSymbol(),
                price.getBestBidPrice(),
                price.getBestAskPrice(),
                price.getBinanceBidPrice(),
                price.getBinanceAskPrice(),
                price.getHuobiBidPrice(),
                price.getHuobiAskPrice(),
                price.getUpdatedAt()
        );
    }
}
