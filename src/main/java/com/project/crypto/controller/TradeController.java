package com.project.crypto.controller;

import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.dto.TradeRequest;
import com.project.crypto.dto.TradeResponse;
import com.project.crypto.dto.TradingPairInfo;
import com.project.crypto.exception.BusinessException;
import com.project.crypto.security.AuthUserPrincipal;
import com.project.crypto.security.SecurityUtils;
import com.project.crypto.service.TradeHistoryService;
import com.project.crypto.service.TradingService;
import com.project.crypto.service.UserContextService;
import com.project.crypto.support.AppLog;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private static final Logger log = LoggerFactory.getLogger(TradeController.class);

    private final UserContextService userContextService;
    private final TradingService tradingService;
    private final TradeHistoryService tradeHistoryService;

    @GetMapping("/pairs")
    public List<TradingPairInfo> supportedPairs() {
        return Arrays.stream(TradingPair.values())
                .map(pair -> new TradingPairInfo(
                        pair.name(),
                        pair.getBaseAsset(),
                        pair.getQuoteAsset(),
                        "Trade %s priced in %s".formatted(pair.getBaseAsset(), pair.getQuoteAsset())))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TradeResponse trade(@Valid @RequestBody TradeRequest request) {
        User user = currentUser();
        try {
            return tradingService.placeOrder(user, request);
        } catch (BusinessException ex) {
            AppLog.warn(
                    log,
                    TradeController.class,
                    "trade",
                    "TradeRequest rejected userId=%s symbol=%s side=%s quantity=%s reason=%s"
                            .formatted(
                                    user.getId(),
                                    request.symbol(),
                                    request.side(),
                                    request.quantity(),
                                    ex.getMessage()));
            throw ex;
        }
    }

    @GetMapping("/history")
    public List<TradeResponse> history(
            @RequestParam(required = false) TradingPair symbol, @RequestParam(defaultValue = "50") int limit) {
        return tradeHistoryService.listByUser(currentUser(), symbol, limit);
    }

    private User currentUser() {
        AuthUserPrincipal principal = SecurityUtils.requirePrincipal();
        return userContextService.requireUser(principal.userId());
    }
}
