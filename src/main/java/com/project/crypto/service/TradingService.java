package com.project.crypto.service;

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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradingService {

    private static final int SCALE = 8;

    private final AggregatedPriceRepository aggregatedPriceRepository;
    private final WalletRepository walletRepository;
    private final TradeTransactionRepository tradeTransactionRepository;

    public TradingService(
            AggregatedPriceRepository aggregatedPriceRepository,
            WalletRepository walletRepository,
            TradeTransactionRepository tradeTransactionRepository) {
        this.aggregatedPriceRepository = aggregatedPriceRepository;
        this.walletRepository = walletRepository;
        this.tradeTransactionRepository = tradeTransactionRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public TradeResponse executeTrade(User user, TradeRequest request) {
        AggregatedPrice aggregatedPrice = aggregatedPriceRepository.findBySymbol(request.symbol())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No aggregated price available for " + request.symbol()));

        BigDecimal executionPrice = resolveExecutionPrice(request.side(), aggregatedPrice);
        BigDecimal normalizedQuantity = request.quantity().setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal quoteAmount = normalizedQuantity
                .multiply(executionPrice)
                .setScale(SCALE, RoundingMode.HALF_UP);

        UserWallets wallets = lockUserWallets(user.getId(), request.symbol());

        if (request.side() == OrderSide.BUY) {
            settleBuy(wallets, normalizedQuantity, quoteAmount);
        } else {
            settleSell(wallets, request.symbol(), normalizedQuantity, quoteAmount);
        }

        TradeTransaction transaction = new TradeTransaction();
        transaction.setUser(user);
        transaction.setSymbol(request.symbol());
        transaction.setSide(request.side());
        transaction.setQuantity(normalizedQuantity);
        transaction.setPrice(executionPrice);
        transaction.setQuoteAmount(quoteAmount);
        transaction.setCreatedAt(Instant.now());

        TradeTransaction saved = tradeTransactionRepository.save(transaction);
        return toResponse(saved);
    }

    private BigDecimal resolveExecutionPrice(OrderSide side, AggregatedPrice aggregatedPrice) {
        return side == OrderSide.BUY
                ? aggregatedPrice.getBestAskPrice()
                : aggregatedPrice.getBestBidPrice();
    }

    /**
     * Locks both wallets in a stable asset order so concurrent trades for the same user
     * cannot deadlock when touching USDT + base asset pairs.
     */
    private UserWallets lockUserWallets(Long userId, TradingPair pair) {
        String baseAsset = pair.getBaseAsset();
        String quoteAsset = pair.getQuoteAsset();

        Wallet first;
        Wallet second;
        if (baseAsset.compareTo(quoteAsset) < 0) {
            first = requireWalletForUpdate(userId, baseAsset);
            second = requireWalletForUpdate(userId, quoteAsset);
            return new UserWallets(first, second);
        }
        first = requireWalletForUpdate(userId, quoteAsset);
        second = requireWalletForUpdate(userId, baseAsset);
        return new UserWallets(second, first);
    }

    private void settleBuy(UserWallets wallets, BigDecimal quantity, BigDecimal quoteAmount) {
        if (wallets.quote().getBalance().compareTo(quoteAmount) < 0) {
            throw new BusinessException("Insufficient USDT balance for buy order");
        }
        wallets.quote().setBalance(wallets.quote().getBalance().subtract(quoteAmount));
        wallets.base().setBalance(wallets.base().getBalance().add(quantity));
    }

    private void settleSell(UserWallets wallets, TradingPair pair, BigDecimal quantity, BigDecimal quoteAmount) {
        if (wallets.base().getBalance().compareTo(quantity) < 0) {
            throw new BusinessException("Insufficient " + pair.getBaseAsset() + " balance for sell order");
        }
        wallets.base().setBalance(wallets.base().getBalance().subtract(quantity));
        wallets.quote().setBalance(wallets.quote().getBalance().add(quoteAmount));
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

    private record UserWallets(Wallet base, Wallet quote) {}
}
