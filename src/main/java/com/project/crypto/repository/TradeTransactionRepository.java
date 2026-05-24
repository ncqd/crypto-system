package com.project.crypto.repository;

import com.project.crypto.domain.entity.TradeTransaction;
import com.project.crypto.domain.enums.TradingPair;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeTransactionRepository extends JpaRepository<TradeTransaction, Long> {

    List<TradeTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<TradeTransaction> findByUserIdAndSymbolOrderByCreatedAtDesc(
            Long userId, TradingPair symbol, Pageable pageable);
}
