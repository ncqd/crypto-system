package com.project.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.project.crypto.domain.entity.AggregatedPrice;
import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.entity.Wallet;
import com.project.crypto.domain.enums.OrderSide;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.dto.TradeRequest;
import com.project.crypto.exception.BusinessException;
import com.project.crypto.repository.AggregatedPriceRepository;
import com.project.crypto.repository.TradeTransactionRepository;
import com.project.crypto.repository.WalletRepository;
import com.project.crypto.service.TradingService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradingServiceTest {

    @Mock
    private AggregatedPriceRepository aggregatedPriceRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TradeTransactionRepository tradeTransactionRepository;

    private TradingService tradingService;

    private final User user = new User();

    @BeforeEach
    void setUp() {
        user.setId(1L);
        tradingService = new TradingService(aggregatedPriceRepository, walletRepository, tradeTransactionRepository);
    }

    @Test
    void executeTrade_buyUsesBestAskPriceAndUpdatesWallets() {
        AggregatedPrice price = new AggregatedPrice();
        price.setSymbol(TradingPair.ETHUSDT);
        price.setBestBidPrice(new BigDecimal("2000.00"));
        price.setBestAskPrice(new BigDecimal("2001.00"));

        Wallet usdtWallet = wallet("USDT", new BigDecimal("50000.00"));
        Wallet ethWallet = wallet("ETH", BigDecimal.ZERO);

        when(aggregatedPriceRepository.findBySymbol(TradingPair.ETHUSDT)).thenReturn(Optional.of(price));
        when(walletRepository.findByUserIdAndAssetForUpdate(1L, "ETH")).thenReturn(Optional.of(ethWallet));
        when(walletRepository.findByUserIdAndAssetForUpdate(1L, "USDT")).thenReturn(Optional.of(usdtWallet));
        when(tradeTransactionRepository.save(any())).thenAnswer(invocation -> {
            com.project.crypto.domain.entity.TradeTransaction tx = invocation.getArgument(0);
            tx.setId(1L);
            return tx;
        });

        var response = tradingService.executeTrade(
                user,
                new TradeRequest(TradingPair.ETHUSDT, OrderSide.BUY, new BigDecimal("1.0"))
        );

        assertThat(response.price()).isEqualByComparingTo("2001.00");
        assertThat(response.quoteAmount()).isEqualByComparingTo("2001.00");
        assertThat(usdtWallet.getBalance()).isEqualByComparingTo("47999.00");
        assertThat(ethWallet.getBalance()).isEqualByComparingTo("1.0");
    }

    @Test
    void executeTrade_sellRejectsInsufficientBalance() {
        AggregatedPrice price = new AggregatedPrice();
        price.setSymbol(TradingPair.BTCUSDT);
        price.setBestBidPrice(new BigDecimal("70000.00"));
        price.setBestAskPrice(new BigDecimal("70010.00"));

        when(aggregatedPriceRepository.findBySymbol(TradingPair.BTCUSDT)).thenReturn(Optional.of(price));
        when(walletRepository.findByUserIdAndAssetForUpdate(1L, "BTC")).thenReturn(Optional.of(wallet("BTC", BigDecimal.ZERO)));
        when(walletRepository.findByUserIdAndAssetForUpdate(1L, "USDT")).thenReturn(Optional.of(wallet("USDT", new BigDecimal("50000.00"))));

        assertThatThrownBy(() -> tradingService.executeTrade(
                user,
                new TradeRequest(TradingPair.BTCUSDT, OrderSide.SELL, new BigDecimal("0.1"))
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient BTC");
    }

    private Wallet wallet(String asset, BigDecimal balance) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setAsset(asset);
        wallet.setBalance(balance);
        return wallet;
    }
}
