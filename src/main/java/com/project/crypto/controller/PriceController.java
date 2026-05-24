package com.project.crypto.controller;

import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.dto.AggregatedPriceResponse;
import com.project.crypto.service.PriceQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceQueryService priceQueryService;

    @GetMapping("/latest")
    public List<AggregatedPriceResponse> getLatestPrices() {
        return priceQueryService.getLatestPrices();
    }

    @GetMapping("/latest/{symbol}")
    public AggregatedPriceResponse getLatestPrice(@PathVariable TradingPair symbol) {
        return priceQueryService.getLatestPrice(symbol);
    }
}
