package com.project.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.crypto.domain.entity.LimitOrder;
import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.entity.Wallet;
import com.project.crypto.domain.enums.OrderSide;
import com.project.crypto.domain.enums.OrderStatus;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.dto.LimitOrderRequest;
import com.project.crypto.exception.BusinessException;
import com.project.crypto.repository.LimitOrderRepository;
import com.project.crypto.repository.WalletRepository;
import com.project.crypto.service.LimitOrderMatcher;
import com.project.crypto.service.LimitOrderService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LimitOrderServiceTest {

    @Mock
    private LimitOrderRepository limitOrderRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LimitOrderMatcher limitOrderMatcher;

    private LimitOrderService limitOrderService;

    private final User user = new User();

    @BeforeEach
    void setUp() {
        user.setId(1L);
        limitOrderService = new LimitOrderService(limitOrderRepository, walletRepository, limitOrderMatcher);
    }

    @Test
    void place_limit_buy_reserves_usdt_and_triggers_match() {
        Wallet usdt = wallet("USDT", new BigDecimal("50000.00"));
        Wallet eth = wallet("ETH", BigDecimal.ZERO);

        when(walletRepository.findByUserIdAndAssetForUpdate(1L, "ETH")).thenReturn(Optional.of(eth));
        when(walletRepository.findByUserIdAndAssetForUpdate(1L, "USDT")).thenReturn(Optional.of(usdt));
        when(limitOrderRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            LimitOrder o = inv.getArgument(0);
            o.setId(10L);
            return o;
        });
        when(limitOrderRepository.findById(10L)).thenAnswer(inv -> {
            LimitOrder o = new LimitOrder();
            o.setId(10L);
            o.setSymbol(TradingPair.ETHUSDT);
            o.setSide(OrderSide.BUY);
            o.setStatus(OrderStatus.PENDING);
            o.setQuantity(new BigDecimal("1.0"));
            o.setLimitPrice(new BigDecimal("1999.00"));
            o.setCreatedAt(Instant.now());
            return Optional.of(o);
        });

        var response = limitOrderService.placeLimitOrder(
                user,
                new LimitOrderRequest(
                        TradingPair.ETHUSDT,
                        OrderSide.BUY,
                        new BigDecimal("1.0"),
                        new BigDecimal("1999.00")));

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(usdt.getBalance()).isEqualByComparingTo("48001.00");
        verify(limitOrderMatcher).tryMatchOrderInNewTransaction(eq(10L));
    }

    @Test
    void buy_limit_rejects_insufficient_usdt() {
        when(walletRepository.findByUserIdAndAssetForUpdate(1L, "ETH"))
                .thenReturn(Optional.of(wallet("ETH", BigDecimal.ZERO)));
        when(walletRepository.findByUserIdAndAssetForUpdate(1L, "USDT"))
                .thenReturn(Optional.of(wallet("USDT", new BigDecimal("100.00"))));

        assertThatThrownBy(() -> limitOrderService.placeLimitOrder(
                        user,
                        new LimitOrderRequest(
                                TradingPair.ETHUSDT,
                                OrderSide.BUY,
                                new BigDecimal("1.0"),
                                new BigDecimal("1999.00"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient USDT");
    }

    private Wallet wallet(String asset, BigDecimal balance) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setAsset(asset);
        wallet.setBalance(balance);
        return wallet;
    }
}
