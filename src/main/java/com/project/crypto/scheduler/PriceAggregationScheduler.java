package com.project.crypto.scheduler;

import com.project.crypto.service.PriceAggregationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PriceAggregationScheduler {

    private final PriceAggregationService priceAggregationService;

    public PriceAggregationScheduler(PriceAggregationService priceAggregationService) {
        this.priceAggregationService = priceAggregationService;
    }

    @Scheduled(
            initialDelayString = "0",
            fixedDelayString = "${crypto.price-aggregation.interval-ms}"
    )
    public void refreshAggregatedPrices() {
        priceAggregationService.aggregateAndStorePrices();
    }
}
