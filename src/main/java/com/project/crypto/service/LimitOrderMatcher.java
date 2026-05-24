package com.project.crypto.service;

import com.project.crypto.config.CryptoProperties;
import com.project.crypto.domain.entity.AggregatedPrice;
import com.project.crypto.domain.entity.LimitOrder;
import com.project.crypto.domain.entity.TradeTransaction;
import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.entity.Wallet;
import com.project.crypto.domain.enums.OrderSide;
import com.project.crypto.domain.enums.OrderStatus;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.exception.BusinessException;
import com.project.crypto.exception.ResourceNotFoundException;
import com.project.crypto.repository.AggregatedPriceRepository;
import com.project.crypto.repository.LimitOrderRepository;
import com.project.crypto.repository.TradeTransactionRepository;
import com.project.crypto.repository.WalletRepository;
import com.project.crypto.support.AppLog;
import com.project.crypto.support.QuoteValidator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LimitOrderMatcher {

    private static final Logger log = LoggerFactory.getLogger(LimitOrderMatcher.class);
    private static final int SCALE = 8;

    private final LimitOrderRepository limitOrderRepository;
    private final AggregatedPriceRepository aggregatedPriceRepository;
    private final WalletRepository walletRepository;
    private final TradeTransactionRepository tradeTransactionRepository;
    private final CryptoProperties cryptoProperties;
    private final TransactionTemplate requiresNewTx;

    public LimitOrderMatcher(
            LimitOrderRepository limitOrderRepository,
            AggregatedPriceRepository aggregatedPriceRepository,
            WalletRepository walletRepository,
            TradeTransactionRepository tradeTransactionRepository,
            CryptoProperties cryptoProperties,
            PlatformTransactionManager transactionManager) {
        this.limitOrderRepository = limitOrderRepository;
        this.aggregatedPriceRepository = aggregatedPriceRepository;
        this.walletRepository = walletRepository;
        this.tradeTransactionRepository = tradeTransactionRepository;
        this.cryptoProperties = cryptoProperties;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public int scanAndMatchPendingOrders() {
        List<Long> pendingIds = limitOrderRepository.findIdsByStatusOrderByCreatedAtAsc(OrderStatus.PENDING);
        if (pendingIds.isEmpty()) {
            return 0;
        }

        AppLog.info(
                log,
                LimitOrderMatcher.class,
                "scanAndMatchPendingOrders",
                "Scanning pendingLimitOrders count=%s".formatted(pendingIds.size()));

        int filled = 0;
        for (Long orderId : pendingIds) {
            if (tryMatchOrderInNewTransaction(orderId)) {
                filled++;
            }
        }

        if (filled > 0) {
            AppLog.info(
                    log,
                    LimitOrderMatcher.class,
                    "scanAndMatchPendingOrders",
                    "MatchedLimitOrders filled=%s scanned=%s".formatted(filled, pendingIds.size()));
        }
        return filled;
    }

    public boolean tryMatchOrderInNewTransaction(Long orderId) {
        Boolean matched = requiresNewTx.execute(status -> {
            try {
                return tryMatchOrder(orderId);
            } catch (Exception ex) {
                AppLog.warn(
                        log,
                        LimitOrderMatcher.class,
                        "tryMatchOrderInNewTransaction",
                        "LimitOrder id=%s match failed reason=%s".formatted(orderId, ex.getMessage()));
                return false;
            }
        });
        return Boolean.TRUE.equals(matched);
    }

    private boolean tryMatchOrder(Long orderId) {
        LimitOrder order = limitOrderRepository
                .findByIdAndStatusForUpdate(orderId, OrderStatus.PENDING)
                .orElse(null);
        if (order == null) {
            return false;
        }

        AggregatedPrice aggregatedPrice = aggregatedPriceRepository
                .findBySymbol(order.getSymbol())
                .orElse(null);
        if (aggregatedPrice == null) {
            AppLog.debug(
                    log,
                    LimitOrderMatcher.class,
                    "tryMatchOrder",
                    "LimitOrder id=%s symbol=%s no price yet"
                            .formatted(orderId, order.getSymbol()));
            return false;
        }
        if (!isPriceFresh(aggregatedPrice)) {
            AppLog.warn(
                    log,
                    LimitOrderMatcher.class,
                    "tryMatchOrder",
                    "LimitOrder id=%s symbol=%s using stale price updatedAt=%s"
                            .formatted(orderId, order.getSymbol(), aggregatedPrice.getUpdatedAt()));
        }

        BigDecimal bestBid = aggregatedPrice.getBestBidPrice();
        BigDecimal bestAsk = aggregatedPrice.getBestAskPrice();

        if (!isTriggered(order, bestBid, bestAsk)) {
            AppLog.info(
                    log,
                    LimitOrderMatcher.class,
                    "tryMatchOrder",
                    "LimitOrder id=%s side=%s limit=%s bid=%s ask=%s not triggered"
                            .formatted(
                                    orderId,
                                    order.getSide(),
                                    order.getLimitPrice(),
                                    bestBid,
                                    bestAsk));
            return false;
        }

        BigDecimal executionPrice = resolveExecutionPrice(order, bestBid, bestAsk);
        fillOrder(order, executionPrice);
        return true;
    }

    private boolean isTriggered(LimitOrder order, BigDecimal bestBid, BigDecimal bestAsk) {
        BigDecimal limit = order.getLimitPrice();
        if (order.getSide() == OrderSide.BUY) {
            if (bestAsk != null && bestAsk.signum() > 0 && bestAsk.compareTo(limit) <= 0) {
                return true;
            }
            return bestBid != null && bestBid.signum() > 0 && bestBid.compareTo(limit) <= 0;
        }
        if (bestBid != null && bestBid.signum() > 0 && bestBid.compareTo(limit) >= 0) {
            return true;
        }
        return bestAsk != null && bestAsk.signum() > 0 && bestAsk.compareTo(limit) >= 0;
    }

    private BigDecimal resolveExecutionPrice(LimitOrder order, BigDecimal bestBid, BigDecimal bestAsk) {
        BigDecimal limit = order.getLimitPrice();
        if (order.getSide() == OrderSide.BUY) {
            if (bestAsk != null && bestAsk.signum() > 0 && bestAsk.compareTo(limit) <= 0) {
                return bestAsk;
            }
            return limit;
        }
        if (bestBid != null && bestBid.signum() > 0 && bestBid.compareTo(limit) >= 0) {
            return bestBid;
        }
        return limit;
    }

    private void fillOrder(LimitOrder order, BigDecimal executionPrice) {
        User user = order.getUser();
        Wallets wallets = lockWallets(user.getId(), order.getSymbol());

        BigDecimal quantity = order.getQuantity();
        BigDecimal quoteAmount = quantity.multiply(executionPrice).setScale(SCALE, RoundingMode.HALF_UP);

        if (order.getSide() == OrderSide.BUY) {
            BigDecimal reserved = order.getReservedQuote();
            if (reserved == null) {
                throw new BusinessException("Limit buy order missing reserved funds");
            }
            if (quoteAmount.compareTo(reserved) > 0) {
                throw new BusinessException("Execution cost exceeds reserved USDT");
            }
            BigDecimal refund = reserved.subtract(quoteAmount);
            wallets.base().setBalance(QuoteValidator.balanceOrZero(wallets.base().getBalance()).add(quantity));
            wallets.quote().setBalance(QuoteValidator.balanceOrZero(wallets.quote().getBalance()).add(refund));
        } else {
            wallets.quote().setBalance(QuoteValidator.balanceOrZero(wallets.quote().getBalance()).add(quoteAmount));
        }

        Instant now = Instant.now();
        order.setStatus(OrderStatus.FILLED);
        order.setExecutionPrice(executionPrice);
        order.setQuoteAmount(quoteAmount);
        order.setFilledAt(now);
        limitOrderRepository.save(order);

        TradeTransaction transaction = new TradeTransaction();
        transaction.setUser(user);
        transaction.setSymbol(order.getSymbol());
        transaction.setSide(order.getSide());
        transaction.setQuantity(quantity);
        transaction.setPrice(executionPrice);
        transaction.setQuoteAmount(quoteAmount);
        transaction.setCreatedAt(now);
        tradeTransactionRepository.save(transaction);

        AppLog.info(
                log,
                LimitOrderMatcher.class,
                "fillOrder",
                "LimitOrder id=%s userId=%s symbol=%s side=%s qty=%s price=%s ETH credited"
                        .formatted(
                                order.getId(),
                                user.getId(),
                                order.getSymbol(),
                                order.getSide(),
                                quantity,
                                executionPrice));
    }

    private boolean isPriceFresh(AggregatedPrice aggregatedPrice) {
        Instant updatedAt = aggregatedPrice.getUpdatedAt();
        if (updatedAt == null) {
            return false;
        }
        long ageMs = Duration.between(updatedAt, Instant.now()).toMillis();
        return ageMs <= cryptoProperties.getMaxPriceAgeMs();
    }

    private Wallets lockWallets(Long userId, TradingPair pair) {
        String baseAsset = pair.getBaseAsset();
        String quoteAsset = pair.getQuoteAsset();

        if (baseAsset.compareTo(quoteAsset) < 0) {
            Wallet base = requireWalletForUpdate(userId, baseAsset);
            Wallet quote = requireWalletForUpdate(userId, quoteAsset);
            return new Wallets(base, quote);
        }
        Wallet quote = requireWalletForUpdate(userId, quoteAsset);
        Wallet base = requireWalletForUpdate(userId, baseAsset);
        return new Wallets(base, quote);
    }

    private Wallet requireWalletForUpdate(Long userId, String asset) {
        return walletRepository
                .findByUserIdAndAssetForUpdate(userId, asset)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for asset: " + asset));
    }

    private record Wallets(Wallet base, Wallet quote) {}
}
