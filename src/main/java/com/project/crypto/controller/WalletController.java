package com.project.crypto.controller;

import com.project.crypto.domain.entity.User;
import com.project.crypto.dto.WalletBalanceResponse;
import com.project.crypto.service.UserContextService;
import com.project.crypto.service.WalletService;
import com.project.crypto.web.CurrentUserId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final UserContextService userContextService;
    private final WalletService walletService;

    public WalletController(UserContextService userContextService, WalletService walletService) {
        this.userContextService = userContextService;
        this.walletService = walletService;
    }

    @GetMapping("/balances")
    public WalletBalanceResponse getBalances(@CurrentUserId Long userId) {
        User user = userContextService.requireUser(userId);
        return walletService.getBalances(user);
    }
}
