package com.project.crypto.scheduler;

import com.project.crypto.service.LimitOrderService;
import com.project.crypto.service.PriceAggregationService;
import com.project.crypto.support.AppLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PriceAggregationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceAggregationScheduler.class);

    private final PriceAggregationService priceAggregationService;
    private final LimitOrderService limitOrderService;

    @Scheduled(
            initialDelayString = "0",
            fixedDelayString = "${crypto.price-aggregation.interval-ms}"
    )
    public void pollPrices() {
        priceAggregationService.updatePrices();
        int filled = limitOrderService.matchPendingOrders();
        AppLog.info(
                log,
                PriceAggregationScheduler.class,
                "pollPrices",
                "PricePoll cycle done matchedOrders=%s".formatted(filled));
    }
}
