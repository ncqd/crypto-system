package com.project.crypto.domain.entity;

import com.project.crypto.domain.enums.TradingPair;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "aggregated_prices")
public class AggregatedPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private TradingPair symbol;

    @Column(name = "best_bid_price", nullable = false, precision = 24, scale = 8)
    private BigDecimal bestBidPrice;

    @Column(name = "best_ask_price", nullable = false, precision = 24, scale = 8)
    private BigDecimal bestAskPrice;

    @Column(name = "binance_bid_price", precision = 24, scale = 8)
    private BigDecimal binanceBidPrice;

    @Column(name = "binance_ask_price", precision = 24, scale = 8)
    private BigDecimal binanceAskPrice;

    @Column(name = "huobi_bid_price", precision = 24, scale = 8)
    private BigDecimal huobiBidPrice;

    @Column(name = "huobi_ask_price", precision = 24, scale = 8)
    private BigDecimal huobiAskPrice;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TradingPair getSymbol() {
        return symbol;
    }

    public void setSymbol(TradingPair symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getBestBidPrice() {
        return bestBidPrice;
    }

    public void setBestBidPrice(BigDecimal bestBidPrice) {
        this.bestBidPrice = bestBidPrice;
    }

    public BigDecimal getBestAskPrice() {
        return bestAskPrice;
    }

    public void setBestAskPrice(BigDecimal bestAskPrice) {
        this.bestAskPrice = bestAskPrice;
    }

    public BigDecimal getBinanceBidPrice() {
        return binanceBidPrice;
    }

    public void setBinanceBidPrice(BigDecimal binanceBidPrice) {
        this.binanceBidPrice = binanceBidPrice;
    }

    public BigDecimal getBinanceAskPrice() {
        return binanceAskPrice;
    }

    public void setBinanceAskPrice(BigDecimal binanceAskPrice) {
        this.binanceAskPrice = binanceAskPrice;
    }

    public BigDecimal getHuobiBidPrice() {
        return huobiBidPrice;
    }

    public void setHuobiBidPrice(BigDecimal huobiBidPrice) {
        this.huobiBidPrice = huobiBidPrice;
    }

    public BigDecimal getHuobiAskPrice() {
        return huobiAskPrice;
    }

    public void setHuobiAskPrice(BigDecimal huobiAskPrice) {
        this.huobiAskPrice = huobiAskPrice;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
