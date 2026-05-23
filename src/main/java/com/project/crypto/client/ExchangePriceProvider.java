package com.project.crypto.client;

import com.project.crypto.client.ExchangePriceClient.ExchangeQuote;
import com.project.crypto.domain.enums.TradingPair;
import java.util.Map;

public interface ExchangePriceProvider {

    Map<TradingPair, ExchangeQuote> fetchBinanceQuotes();

    Map<TradingPair, ExchangeQuote> fetchHuobiQuotes();
}
