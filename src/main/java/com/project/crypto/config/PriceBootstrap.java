package com.project.crypto.config;

import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.repository.AggregatedPriceRepository;
import com.project.crypto.service.LimitOrderMatcher;
import com.project.crypto.service.PriceAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PriceBootstrap implements CommandLineRunner {

    private final PriceAggregationService priceAggregationService;
    private final LimitOrderMatcher limitOrderMatcher;
    private final AggregatedPriceRepository aggregatedPriceRepository;

    @Override
    public void run(String... args) {
        boolean needsSeed = aggregatedPriceRepository.count() == 0;
        if (!needsSeed) {
            for (TradingPair pair : TradingPair.values()) {
                if (aggregatedPriceRepository.findBySymbol(pair).isEmpty()) {
                    needsSeed = true;
                    break;
                }
            }
        }
        if (needsSeed) {
            priceAggregationService.updatePrices();
            limitOrderMatcher.scanAndMatchPendingOrders();
        }
    }
}
