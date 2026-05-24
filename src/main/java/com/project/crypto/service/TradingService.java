package com.project.crypto.service;

import com.project.crypto.config.CryptoProperties;
import com.project.crypto.domain.entity.AggregatedPrice;
import com.project.crypto.domain.entity.TradeTransaction;
import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.entity.Wallet;
import com.project.crypto.domain.enums.OrderSide;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.dto.TradeRequest;
import com.project.crypto.dto.TradeResponse;
import com.project.crypto.exception.BusinessException;
import com.project.crypto.exception.ResourceNotFoundException;
import com.project.crypto.repository.AggregatedPriceRepository;
import com.project.crypto.repository.TradeTransactionRepository;
import com.project.crypto.repository.WalletRepository;
import com.project.crypto.support.AppLog;
import com.project.crypto.support.QuoteValidator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TradingService {

    private static final Logger log = LoggerFactory.getLogger(TradingService.class);
    private static final int SCALE = 8;

    private final AggregatedPriceRepository aggregatedPriceRepository;
    private final WalletRepository walletRepository;
    private final TradeTransactionRepository tradeTransactionRepository;
    private final CryptoProperties cryptoProperties;

    @Transactional(rollbackFor = Exception.class)
    public TradeResponse placeOrder(User user, TradeRequest request) {
        requireUser(user);

        AppLog.info(
                log,
                TradingService.class,
                "placeOrder",
                "TradeRequest userId=%s symbol=%s side=%s quantity=%s"
                        .formatted(user.getId(), request.symbol(), request.side(), request.quantity()));

        AggregatedPrice aggregatedPrice = aggregatedPriceRepository.findBySymbol(request.symbol())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No aggregated price available for " + request.symbol()));

        requireFreshPrice(aggregatedPrice);

        BigDecimal executionPrice = requirePrice(priceForSide(request.side(), aggregatedPrice), request.side());
        BigDecimal quantity = request.quantity().setScale(SCALE, RoundingMode.HALF_UP);
        if (quantity.signum() <= 0) {
            throw new BusinessException("Quantity must be positive");
        }

        BigDecimal quoteAmount = quantity
                .multiply(executionPrice)
                .setScale(SCALE, RoundingMode.HALF_UP);

        Wallets wallets = lockWallets(user.getId(), request.symbol());

        if (request.side() == OrderSide.BUY) {
            applyBuy(wallets, quantity, quoteAmount);
        } else {
            applySell(wallets, request.symbol(), quantity, quoteAmount);
        }

        TradeTransaction transaction = new TradeTransaction();
        transaction.setUser(user);
        transaction.setSymbol(request.symbol());
        transaction.setSide(request.side());
        transaction.setQuantity(quantity);
        transaction.setPrice(executionPrice);
        transaction.setQuoteAmount(quoteAmount);
        transaction.setCreatedAt(Instant.now());

        TradeTransaction saved = tradeTransactionRepository.save(transaction);

        AppLog.info(
                log,
                TradingService.class,
                "placeOrder",
                "TradeTransaction id=%s userId=%s symbol=%s side=%s quantity=%s price=%s quoteAmount=%s"
                        .formatted(
                                saved.getId(),
                                user.getId(),
                                saved.getSymbol(),
                                saved.getSide(),
                                saved.getQuantity(),
                                saved.getPrice(),
                                saved.getQuoteAmount()));

        return toResponse(saved);
    }

    private void requireUser(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException("Invalid user");
        }
    }

    private void requireFreshPrice(AggregatedPrice aggregatedPrice) {
        Instant updatedAt = aggregatedPrice.getUpdatedAt();
        if (updatedAt == null) {
            throw new BusinessException("Price timestamp missing");
        }
        long ageMs = Duration.between(updatedAt, Instant.now()).toMillis();
        if (ageMs > cryptoProperties.getMaxPriceAgeMs()) {
            throw new BusinessException("Price data is stale");
        }
    }

    private BigDecimal priceForSide(OrderSide side, AggregatedPrice aggregatedPrice) {
        return side == OrderSide.BUY
                ? aggregatedPrice.getBestAskPrice()
                : aggregatedPrice.getBestBidPrice();
    }

    private BigDecimal requirePrice(BigDecimal price, OrderSide side) {
        if (price == null || price.signum() <= 0) {
            throw new BusinessException("Invalid execution price for " + side + " order");
        }
        return price;
    }

    private Wallets lockWallets(Long userId, TradingPair pair) {
        String baseAsset = pair.getBaseAsset();
        String quoteAsset = pair.getQuoteAsset();

        Wallet first;
        Wallet second;
        if (baseAsset.compareTo(quoteAsset) < 0) {
            first = requireWalletForUpdate(userId, baseAsset);
            second = requireWalletForUpdate(userId, quoteAsset);
            return new Wallets(first, second);
        }
        first = requireWalletForUpdate(userId, quoteAsset);
        second = requireWalletForUpdate(userId, baseAsset);
        return new Wallets(second, first);
    }

    private void applyBuy(Wallets wallets, BigDecimal quantity, BigDecimal quoteAmount) {
        if (QuoteValidator.balanceOrZero(wallets.quote().getBalance()).compareTo(quoteAmount) < 0) {
            throw new BusinessException("Insufficient USDT balance for buy order");
        }
        wallets.quote().setBalance(QuoteValidator.balanceOrZero(wallets.quote().getBalance()).subtract(quoteAmount));
        wallets.base().setBalance(QuoteValidator.balanceOrZero(wallets.base().getBalance()).add(quantity));
    }

    private void applySell(Wallets wallets, TradingPair pair, BigDecimal quantity, BigDecimal quoteAmount) {
        if (QuoteValidator.balanceOrZero(wallets.base().getBalance()).compareTo(quantity) < 0) {
            throw new BusinessException("Insufficient " + pair.getBaseAsset() + " balance for sell order");
        }
        wallets.base().setBalance(QuoteValidator.balanceOrZero(wallets.base().getBalance()).subtract(quantity));
        wallets.quote().setBalance(QuoteValidator.balanceOrZero(wallets.quote().getBalance()).add(quoteAmount));
    }

    private Wallet requireWalletForUpdate(Long userId, String asset) {
        return walletRepository.findByUserIdAndAssetForUpdate(userId, asset)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for asset: " + asset));
    }

    private TradeResponse toResponse(TradeTransaction transaction) {
        return new TradeResponse(
                transaction.getId(),
                transaction.getSymbol(),
                transaction.getSide(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getQuoteAmount(),
                transaction.getCreatedAt()
        );
    }

    private record Wallets(Wallet base, Wallet quote) {}
}
