package com.project.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.crypto.domain.entity.TradeTransaction;
import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.enums.OrderSide;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.repository.TradeTransactionRepository;
import com.project.crypto.service.TradeHistoryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TradeHistoryServiceTest {

    @Mock
    private TradeTransactionRepository tradeTransactionRepository;

    @InjectMocks
    private TradeHistoryService tradeHistoryService;

    @Test
    void listByUser_filtersBySymbolAndCapsLimit() {
        User user = new User();
        user.setId(1L);

        TradeTransaction tx = new TradeTransaction();
        tx.setId(10L);
        tx.setSymbol(TradingPair.ETHUSDT);
        tx.setSide(OrderSide.BUY);
        tx.setQuantity(BigDecimal.ONE);
        tx.setPrice(new BigDecimal("2000"));
        tx.setQuoteAmount(new BigDecimal("2000"));
        tx.setCreatedAt(Instant.now());

        when(tradeTransactionRepository.findByUserIdAndSymbolOrderByCreatedAtDesc(
                        eq(1L), eq(TradingPair.ETHUSDT), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(tx));

        var result = tradeHistoryService.listByUser(user, TradingPair.ETHUSDT, 500);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(10L);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(tradeTransactionRepository)
                .findByUserIdAndSymbolOrderByCreatedAtDesc(eq(1L), eq(TradingPair.ETHUSDT), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(200);
    }
}
