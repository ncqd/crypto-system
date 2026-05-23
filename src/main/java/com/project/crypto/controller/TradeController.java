package com.project.crypto.controller;

import com.project.crypto.domain.entity.User;
import com.project.crypto.dto.TradeRequest;
import com.project.crypto.dto.TradeResponse;
import com.project.crypto.service.TradeHistoryService;
import com.project.crypto.service.TradingService;
import com.project.crypto.service.UserContextService;
import com.project.crypto.web.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final UserContextService userContextService;
    private final TradingService tradingService;
    private final TradeHistoryService tradeHistoryService;

    public TradeController(
            UserContextService userContextService,
            TradingService tradingService,
            TradeHistoryService tradeHistoryService) {
        this.userContextService = userContextService;
        this.tradingService = tradingService;
        this.tradeHistoryService = tradeHistoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TradeResponse trade(@CurrentUserId Long userId, @Valid @RequestBody TradeRequest request) {
        User user = userContextService.requireUser(userId);
        return tradingService.executeTrade(user, request);
    }

    @GetMapping("/history")
    public List<TradeResponse> getTradeHistory(@CurrentUserId Long userId) {
        User user = userContextService.requireUser(userId);
        return tradeHistoryService.getTradeHistory(user);
    }
}
