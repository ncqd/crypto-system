package com.project.crypto.repository;

import com.project.crypto.domain.entity.TradeTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeTransactionRepository extends JpaRepository<TradeTransaction, Long> {

    List<TradeTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
