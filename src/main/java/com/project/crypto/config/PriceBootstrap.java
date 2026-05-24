package com.project.crypto.config;

import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.repository.AggregatedPriceRepository;
import com.project.crypto.service.PriceAggregationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PriceBootstrap implements CommandLineRunner {

    private final PriceAggregationService priceAggregationService;
    private final AggregatedPriceRepository aggregatedPriceRepository;

    public PriceBootstrap(
            PriceAggregationService priceAggregationService,
            AggregatedPriceRepository aggregatedPriceRepository) {
        this.priceAggregationService = priceAggregationService;
        this.aggregatedPriceRepository = aggregatedPriceRepository;
    }

    @Override
    public void run(String... args) {
        if (aggregatedPriceRepository.count() == 0) {
            priceAggregationService.updatePrices();
            return;
        }
        for (TradingPair pair : TradingPair.values()) {
            if (aggregatedPriceRepository.findBySymbol(pair).isEmpty()) {
                priceAggregationService.updatePrices();
                return;
            }
        }
    }
}
