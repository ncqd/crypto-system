package com.project.crypto.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crypto.market")
public class MarketPriceProperties {

    private Map<String, Quote> primary = defaultPrimary();
    private Map<String, Quote> secondary = defaultSecondary();

    public Map<String, Quote> getPrimary() {
        return primary;
    }

    public void setPrimary(Map<String, Quote> primary) {
        this.primary = primary;
    }

    public Map<String, Quote> getSecondary() {
        return secondary;
    }

    public void setSecondary(Map<String, Quote> secondary) {
        this.secondary = secondary;
    }

    private static Map<String, Quote> defaultPrimary() {
        Map<String, Quote> quotes = new HashMap<>();
        quotes.put("ETHUSDT", new Quote(new BigDecimal("2000.00000000"), new BigDecimal("2001.00000000")));
        quotes.put("BTCUSDT", new Quote(new BigDecimal("70000.00000000"), new BigDecimal("70010.00000000")));
        return quotes;
    }

    private static Map<String, Quote> defaultSecondary() {
        Map<String, Quote> quotes = new HashMap<>();
        quotes.put("ETHUSDT", new Quote(new BigDecimal("2000.50000000"), new BigDecimal("2000.80000000")));
        quotes.put("BTCUSDT", new Quote(new BigDecimal("69990.00000000"), new BigDecimal("70005.00000000")));
        return quotes;
    }

    public static class Quote {
        private BigDecimal bid;
        private BigDecimal ask;

        public Quote() {}

        public Quote(BigDecimal bid, BigDecimal ask) {
            this.bid = bid;
            this.ask = ask;
        }

        public BigDecimal getBid() {
            return bid;
        }

        public void setBid(BigDecimal bid) {
            this.bid = bid;
        }

        public BigDecimal getAsk() {
            return ask;
        }

        public void setAsk(BigDecimal ask) {
            this.ask = ask;
        }
    }
}
