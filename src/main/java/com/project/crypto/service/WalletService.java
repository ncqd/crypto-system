package com.project.crypto.service;

import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.entity.Wallet;
import com.project.crypto.dto.WalletBalanceResponse;
import com.project.crypto.dto.WalletTransferRequest;
import com.project.crypto.exception.BusinessException;
import com.project.crypto.exception.ResourceNotFoundException;
import com.project.crypto.repository.WalletRepository;
import com.project.crypto.support.AppLog;
import com.project.crypto.support.QuoteValidator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);
    private static final int SCALE = 8;
    private static final Set<String> SUPPORTED_ASSETS = Set.of("USDT", "ETH", "BTC");

    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public WalletBalanceResponse getBalances(User user) {
        return toBalanceResponse(user);
    }

    @Transactional
    public WalletBalanceResponse deposit(User user, WalletTransferRequest request) {
        BigDecimal amount = normalizeAmount(request.amount());
        Wallet wallet = lockWallet(user.getId(), request.asset().toUpperCase());
        wallet.setBalance(QuoteValidator.balanceOrZero(wallet.getBalance()).add(amount));
        walletRepository.save(wallet);

        AppLog.info(
                log,
                WalletService.class,
                "deposit",
                "Wallet userId=%s asset=%s amount=%s".formatted(user.getId(), wallet.getAsset(), amount));

        return toBalanceResponse(user);
    }

    @Transactional
    public WalletBalanceResponse withdraw(User user, WalletTransferRequest request) {
        BigDecimal amount = normalizeAmount(request.amount());
        Wallet wallet = lockWallet(user.getId(), request.asset().toUpperCase());
        BigDecimal balance = QuoteValidator.balanceOrZero(wallet.getBalance());
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException("Insufficient " + wallet.getAsset() + " balance for withdrawal");
        }
        wallet.setBalance(balance.subtract(amount));
        walletRepository.save(wallet);

        AppLog.info(
                log,
                WalletService.class,
                "withdraw",
                "Wallet userId=%s asset=%s amount=%s".formatted(user.getId(), wallet.getAsset(), amount));

        return toBalanceResponse(user);
    }

    private Wallet lockWallet(Long userId, String asset) {
        if (!SUPPORTED_ASSETS.contains(asset)) {
            throw new BusinessException("Unsupported asset: " + asset);
        }
        return walletRepository
                .findByUserIdAndAssetForUpdate(userId, asset)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for asset: " + asset));
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private WalletBalanceResponse toBalanceResponse(User user) {
        List<WalletBalanceResponse.AssetBalance> balances = walletRepository.findByUserId(user.getId()).stream()
                .map(wallet -> new WalletBalanceResponse.AssetBalance(wallet.getAsset(), wallet.getBalance()))
                .toList();
        return new WalletBalanceResponse(user.getId(), balances);
    }
}
