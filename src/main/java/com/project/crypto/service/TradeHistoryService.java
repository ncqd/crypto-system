package com.project.crypto.service;

import com.project.crypto.domain.entity.TradeTransaction;
import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.dto.TradeResponse;
import com.project.crypto.repository.TradeTransactionRepository;
import com.project.crypto.support.AppLog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TradeHistoryService {

    private static final Logger log = LoggerFactory.getLogger(TradeHistoryService.class);
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final TradeTransactionRepository tradeTransactionRepository;

    public List<TradeResponse> listByUser(User user, TradingPair symbol, int limit) {
        int pageSize = normalizeLimit(limit);
        Pageable pageable = PageRequest.of(0, pageSize);

        List<TradeTransaction> rows = symbol == null
                ? tradeTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                : tradeTransactionRepository.findByUserIdAndSymbolOrderByCreatedAtDesc(
                        user.getId(), symbol, pageable);

        AppLog.info(
                log,
                TradeHistoryService.class,
                "listByUser",
                "TradeHistory userId=%s symbol=%s limit=%s returned=%s"
                        .formatted(user.getId(), symbol, pageSize, rows.size()));

        return rows.stream().map(this::toResponse).toList();
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
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
