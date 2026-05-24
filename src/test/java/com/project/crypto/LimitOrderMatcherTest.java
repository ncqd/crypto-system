package com.project.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.project.crypto.config.CryptoProperties;
import com.project.crypto.domain.entity.AggregatedPrice;
import com.project.crypto.domain.entity.LimitOrder;
import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.entity.Wallet;
import com.project.crypto.domain.enums.OrderSide;
import com.project.crypto.domain.enums.OrderStatus;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.repository.AggregatedPriceRepository;
import com.project.crypto.repository.LimitOrderRepository;
import com.project.crypto.repository.TradeTransactionRepository;
import com.project.crypto.repository.WalletRepository;
import com.project.crypto.service.LimitOrderMatcher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.ResourcelessTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class LimitOrderMatcherTest {

    @Mock
    private LimitOrderRepository limitOrderRepository;

    @Mock
    private AggregatedPriceRepository aggregatedPriceRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TradeTransactionRepository tradeTransactionRepository;

    @Mock
    private CryptoProperties cryptoProperties;

    private LimitOrderMatcher limitOrderMatcher;

    private final User user = new User();

    @BeforeEach
    void setUp() {
        user.setId(1L);
        when(cryptoProperties.getMaxPriceAgeMs()).thenReturn(30_000L);
        TransactionTemplate transactionTemplate = new TransactionTemplate(new ResourcelessTransactionManager());
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        limitOrderMatcher = new LimitOrderMatcher(
                limitOrderRepository,
                aggregatedPriceRepository,
                walletRepository,
                tradeTransactionRepository,
                cryptoProperties,
                transactionTemplate);
    }

    @Test
    void buy_limit_fills_when_ask_at_or_below_limit() {
        Wallet usdt = wallet("USDT", new BigDecimal("50000.00"));
        Wallet eth = wallet("ETH", BigDecimal.ZERO);

        LimitOrder order = pendingBuyOrder("1999.00");
        order.setId(10L);

        when(limitOrderRepository.findByIdAndStatusForUpdate(10L, OrderStatus.PENDING)).thenReturn(Optional.of(order));
        when(aggregatedPriceRepository.findBySymbol(TradingPair.ETHUSDT))
                .thenReturn(Optional.of(freshPrice("1998.00", "1999.00")));
        when(walletRepository.findByUserIdAndAssetForUpdate(1L, "ETH")).thenReturn(Optional.of(eth));
        when(walletRepository.findByUserIdAndAssetForUpdate(1L, "USDT")).thenReturn(Optional.of(usdt));
        when(limitOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tradeTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean filled = limitOrderMatcher.tryMatchOrderInNewTransaction(10L);

        assertThat(filled).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(eth.getBalance()).isEqualByComparingTo("1.0");
    }

    @Test
    void buy_limit_not_filled_when_bid_and_ask_above_limit() {
        LimitOrder order = pendingBuyOrder("1999.00");
        order.setId(10L);

        when(limitOrderRepository.findByIdAndStatusForUpdate(10L, OrderStatus.PENDING)).thenReturn(Optional.of(order));
        when(aggregatedPriceRepository.findBySymbol(TradingPair.ETHUSDT))
                .thenReturn(Optional.of(freshPrice("2000.00", "2001.00")));

        boolean filled = limitOrderMatcher.tryMatchOrderInNewTransaction(10L);

        assertThat(filled).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void buy_limit_fills_when_bid_at_or_below_limit_even_if_ask_above() {
        Wallet usdt = wallet("USDT", new BigDecimal("50000.00"));
        Wallet eth = wallet("ETH", BigDecimal.ZERO);

        LimitOrder order = pendingBuyOrder("2022.70");
        order.setReservedQuote(new BigDecimal("2022.70"));
        order.setId(11L);

        when(limitOrderRepository.findByIdAndStatusForUpdate(11L, OrderStatus.PENDING)).thenReturn(Optional.of(order));
        when(aggregatedPriceRepository.findBySymbol(TradingPair.ETHUSDT))
                .thenReturn(Optional.of(freshPrice("2021.00", "2025.00")));
        when(walletRepository.findByUserIdAndAssetForUpdate(1L, "ETH")).thenReturn(Optional.of(eth));
        when(walletRepository.findByUserIdAndAssetForUpdate(1L, "USDT")).thenReturn(Optional.of(usdt));
        when(limitOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tradeTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean filled = limitOrderMatcher.tryMatchOrderInNewTransaction(11L);

        assertThat(filled).isTrue();
        assertThat(order.getExecutionPrice()).isEqualByComparingTo("2022.70");
        assertThat(eth.getBalance()).isEqualByComparingTo("1.0");
    }

    private LimitOrder pendingBuyOrder(String limitPrice) {
        LimitOrder order = new LimitOrder();
        order.setUser(user);
        order.setSymbol(TradingPair.ETHUSDT);
        order.setSide(OrderSide.BUY);
        order.setStatus(OrderStatus.PENDING);
        order.setQuantity(new BigDecimal("1.0"));
        order.setLimitPrice(new BigDecimal(limitPrice));
        order.setReservedQuote(new BigDecimal(limitPrice));
        order.setCreatedAt(Instant.now());
        return order;
    }

    private AggregatedPrice freshPrice(String bid, String ask) {
        AggregatedPrice price = new AggregatedPrice();
        price.setSymbol(TradingPair.ETHUSDT);
        price.setBestBidPrice(new BigDecimal(bid));
        price.setBestAskPrice(new BigDecimal(ask));
        price.setUpdatedAt(Instant.now());
        return price;
    }

    private Wallet wallet(String asset, BigDecimal balance) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setAsset(asset);
        wallet.setBalance(balance);
        return wallet;
    }
}
