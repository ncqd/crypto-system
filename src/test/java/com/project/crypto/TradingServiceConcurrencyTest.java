package com.project.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.crypto.domain.entity.AggregatedPrice;
import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.entity.Wallet;
import com.project.crypto.domain.enums.OrderSide;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.dto.TradeRequest;
import com.project.crypto.exception.BusinessException;
import com.project.crypto.repository.AggregatedPriceRepository;
import com.project.crypto.repository.TradeTransactionRepository;
import com.project.crypto.repository.UserRepository;
import com.project.crypto.repository.WalletRepository;
import com.project.crypto.service.TradingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TradingServiceConcurrencyTest {

    private static final int CONCURRENT_ATTEMPTS = 8;

    @Autowired
    private TradingService tradingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AggregatedPriceRepository aggregatedPriceRepository;

    @Autowired
    private TradeTransactionRepository tradeTransactionRepository;

    private User user;

    @BeforeEach
    void setUp() {
        tradeTransactionRepository.deleteAll();
        walletRepository.deleteAll();
        aggregatedPriceRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(createUser("concurrency-trader"));

        saveWallet(user, "USDT", new BigDecimal("3000.00000000"));
        saveWallet(user, "ETH", BigDecimal.ZERO);

        AggregatedPrice price = new AggregatedPrice();
        price.setSymbol(TradingPair.ETHUSDT);
        price.setBestBidPrice(new BigDecimal("2000.00000000"));
        price.setBestAskPrice(new BigDecimal("2001.00000000"));
        price.setUpdatedAt(Instant.now());
        aggregatedPriceRepository.save(price);
    }

    @Test
    void parallel_buy_eth() throws Exception {
        TradeRequest request =
                new TradeRequest(TradingPair.ETHUSDT, OrderSide.BUY, new BigDecimal("1.00000000"));

        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_ATTEMPTS)) {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_ATTEMPTS; i++) {
                tasks.add(
                        () -> {
                            try {
                                User trader = userRepository.findById(user.getId()).orElseThrow();
                                tradingService.placeOrder(trader, request);
                                return true;
                            } catch (BusinessException ex) {
                                return false;
                            }
                        });
            }

            List<Future<Boolean>> results = executor.invokeAll(tasks);
            long successes = results.stream().filter(Future::isDone).filter(this::successfulTrade).count();

            Wallet usdt = walletRepository.findByUserIdAndAsset(user.getId(), "USDT").orElseThrow();
            Wallet eth = walletRepository.findByUserIdAndAsset(user.getId(), "ETH").orElseThrow();

            assertThat(successes).isEqualTo(1);
            assertThat(usdt.getBalance()).isEqualByComparingTo("999.00000000");
            assertThat(eth.getBalance()).isEqualByComparingTo("1.00000000");
            assertThat(usdt.getBalance().signum()).isGreaterThanOrEqualTo(0);
            assertThat(eth.getBalance().signum()).isGreaterThanOrEqualTo(0);
            assertThat(tradeTransactionRepository.count()).isEqualTo(1);
        }
    }

    private boolean successfulTrade(Future<Boolean> future) {
        try {
            return future.get();
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private User createUser(String username) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPasswordHash("test-hash");
        newUser.setCreatedAt(Instant.now());
        return newUser;
    }

    private void saveWallet(User owner, String asset, BigDecimal balance) {
        Wallet wallet = new Wallet();
        wallet.setUser(owner);
        wallet.setAsset(asset);
        wallet.setBalance(balance);
        walletRepository.save(wallet);
    }
}
