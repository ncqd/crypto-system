package com.project.crypto.controller;

import com.project.crypto.domain.entity.User;
import com.project.crypto.dto.WalletBalanceResponse;
import com.project.crypto.dto.WalletTransferRequest;
import com.project.crypto.security.AuthUserPrincipal;
import com.project.crypto.security.SecurityUtils;
import com.project.crypto.service.UserContextService;
import com.project.crypto.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final UserContextService userContextService;
    private final WalletService walletService;

    @GetMapping("/balances")
    public WalletBalanceResponse getBalances() {
        return walletService.getBalances(currentUser());
    }

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.OK)
    public WalletBalanceResponse deposit(@Valid @RequestBody WalletTransferRequest request) {
        return walletService.deposit(currentUser(), request);
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.OK)
    public WalletBalanceResponse withdraw(@Valid @RequestBody WalletTransferRequest request) {
        return walletService.withdraw(currentUser(), request);
    }

    private User currentUser() {
        AuthUserPrincipal principal = SecurityUtils.requirePrincipal();
        return userContextService.requireUser(principal.userId());
    }
}
