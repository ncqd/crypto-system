package com.project.crypto.config;

import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.entity.Wallet;
import com.project.crypto.repository.UserRepository;
import com.project.crypto.repository.WalletRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_USERNAME = "demo-user";
    private static final BigDecimal INITIAL_USDT_BALANCE = new BigDecimal("50000.00000000");

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public DataInitializer(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    public void run(String... args) {
        User user = userRepository.findByUsername(DEFAULT_USERNAME)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(DEFAULT_USERNAME);
                    return userRepository.save(newUser);
                });

        ensureWallet(user, "USDT", INITIAL_USDT_BALANCE);
        ensureWallet(user, "ETH", BigDecimal.ZERO);
        ensureWallet(user, "BTC", BigDecimal.ZERO);
    }

    private void ensureWallet(User user, String asset, BigDecimal balance) {
        walletRepository.findByUserIdAndAsset(user.getId(), asset)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUser(user);
                    wallet.setAsset(asset);
                    wallet.setBalance(balance);
                    return walletRepository.save(wallet);
                });
    }
}
