package com.project.crypto.service;

import com.project.crypto.domain.entity.User;
import com.project.crypto.dto.WalletBalanceResponse;
import com.project.crypto.repository.WalletRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public WalletBalanceResponse getBalances(User user) {
        List<WalletBalanceResponse.AssetBalance> balances = walletRepository.findByUserId(user.getId()).stream()
                .map(wallet -> new WalletBalanceResponse.AssetBalance(wallet.getAsset(), wallet.getBalance()))
                .toList();

        return new WalletBalanceResponse(user.getId(), balances);
    }
}
