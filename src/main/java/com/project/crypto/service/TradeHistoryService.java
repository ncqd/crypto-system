package com.project.crypto.service;

import com.project.crypto.domain.entity.TradeTransaction;
import com.project.crypto.domain.entity.User;
import com.project.crypto.dto.TradeResponse;
import com.project.crypto.repository.TradeTransactionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TradeHistoryService {

    private final TradeTransactionRepository tradeTransactionRepository;

    public TradeHistoryService(TradeTransactionRepository tradeTransactionRepository) {
        this.tradeTransactionRepository = tradeTransactionRepository;
    }

    public List<TradeResponse> getTradeHistory(User user) {
        return tradeTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private TradeResponse toResponse(TradeTransaction transaction) {
        return new TradeResponse(
                transaction.getId(),
                transaction.getSymbol(),
                transaction.getSide(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getQuoteAmount(),
                transaction.getCreatedAt()
        );
    }
}
