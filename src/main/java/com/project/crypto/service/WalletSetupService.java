package com.project.crypto.service;

import com.project.crypto.config.WalletProperties;
import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.entity.Wallet;
import com.project.crypto.repository.WalletRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletSetupService {

    private final WalletRepository walletRepository;
    private final WalletProperties walletProperties;

    @Transactional
    public void createWalletsForUser(User user) {
        createWallet(user, "USDT", walletProperties.getInitialUsdtBalance());
        createWallet(user, "ETH", BigDecimal.ZERO);
        createWallet(user, "BTC", BigDecimal.ZERO);
    }

    private void createWallet(User user, String asset, BigDecimal balance) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setAsset(asset);
        wallet.setBalance(balance);
        walletRepository.save(wallet);
    }
}
