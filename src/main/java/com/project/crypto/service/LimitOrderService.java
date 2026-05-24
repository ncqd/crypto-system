package com.project.crypto.service;

import com.project.crypto.domain.entity.LimitOrder;
import com.project.crypto.domain.entity.User;
import com.project.crypto.domain.entity.Wallet;
import com.project.crypto.domain.enums.OrderSide;
import com.project.crypto.domain.enums.OrderStatus;
import com.project.crypto.domain.enums.TradingPair;
import com.project.crypto.dto.LimitOrderRequest;
import com.project.crypto.dto.LimitOrderResponse;
import com.project.crypto.exception.BusinessException;
import com.project.crypto.exception.ResourceNotFoundException;
import com.project.crypto.repository.LimitOrderRepository;
import com.project.crypto.repository.WalletRepository;
import com.project.crypto.support.AppLog;
import com.project.crypto.support.QuoteValidator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LimitOrderService {

    private static final Logger log = LoggerFactory.getLogger(LimitOrderService.class);
    private static final int SCALE = 8;

    private final LimitOrderRepository limitOrderRepository;
    private final WalletRepository walletRepository;
    private final LimitOrderMatcher limitOrderMatcher;

    @Transactional(rollbackFor = Exception.class)
    public LimitOrderResponse placeLimitOrder(User user, LimitOrderRequest request) {
        requireUser(user);

        BigDecimal quantity = request.quantity().setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal limitPrice = request.limitPrice().setScale(SCALE, RoundingMode.HALF_UP);
        if (quantity.signum() <= 0 || limitPrice.signum() <= 0) {
            throw new BusinessException("Quantity and limit price must be positive");
        }

        AppLog.info(
                log,
                LimitOrderService.class,
                "placeLimitOrder",
                "LimitOrderRequest userId=%s symbol=%s side=%s quantity=%s limitPrice=%s"
                        .formatted(user.getId(), request.symbol(), request.side(), quantity, limitPrice));

        Wallets wallets = lockWallets(user.getId(), request.symbol());

        LimitOrder order = new LimitOrder();
        order.setUser(user);
        order.setSymbol(request.symbol());
        order.setSide(request.side());
        order.setStatus(OrderStatus.PENDING);
        order.setQuantity(quantity);
        order.setLimitPrice(limitPrice);
        order.setCreatedAt(Instant.now());

        if (request.side() == OrderSide.BUY) {
            BigDecimal reserved = quantity.multiply(limitPrice).setScale(SCALE, RoundingMode.HALF_UP);
            if (QuoteValidator.balanceOrZero(wallets.quote().getBalance()).compareTo(reserved) < 0) {
                throw new BusinessException("Insufficient USDT balance for limit buy order");
            }
            wallets.quote().setBalance(QuoteValidator.balanceOrZero(wallets.quote().getBalance()).subtract(reserved));
            order.setReservedQuote(reserved);
        } else {
            if (QuoteValidator.balanceOrZero(wallets.base().getBalance()).compareTo(quantity) < 0) {
                throw new BusinessException(
                        "Insufficient " + request.symbol().getBaseAsset() + " balance for limit sell order");
            }
            wallets.base().setBalance(QuoteValidator.balanceOrZero(wallets.base().getBalance()).subtract(quantity));
        }

        LimitOrder saved = limitOrderRepository.save(order);
        limitOrderMatcher.tryMatchOrderInNewTransaction(saved.getId());

        return toResponse(limitOrderRepository.findById(saved.getId()).orElse(saved));
    }

    @Transactional(rollbackFor = Exception.class)
    public LimitOrderResponse cancelLimitOrder(User user, Long orderId) {
        requireUser(user);

        LimitOrder order = limitOrderRepository
                .findByIdAndUserIdForUpdate(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Limit order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Only pending limit orders can be cancelled");
        }

        Wallets wallets = lockWallets(user.getId(), order.getSymbol());
        releaseReservation(order, wallets);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        LimitOrder saved = limitOrderRepository.save(order);

        AppLog.info(
                log,
                LimitOrderService.class,
                "cancelLimitOrder",
                "LimitOrder id=%s userId=%s cancelled".formatted(orderId, user.getId()));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LimitOrderResponse> listOrders(User user, OrderStatus status) {
        requireUser(user);
        List<LimitOrder> orders = status == null
                ? limitOrderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                : limitOrderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status);
        return orders.stream().map(this::toResponse).toList();
    }

    public int matchPendingOrders() {
        return limitOrderMatcher.scanAndMatchPendingOrders();
    }

    private void releaseReservation(LimitOrder order, Wallets wallets) {
        if (order.getSide() == OrderSide.BUY) {
            BigDecimal reserved = order.getReservedQuote();
            if (reserved != null) {
                wallets.quote().setBalance(QuoteValidator.balanceOrZero(wallets.quote().getBalance()).add(reserved));
            }
        } else {
            wallets.base().setBalance(QuoteValidator.balanceOrZero(wallets.base().getBalance()).add(order.getQuantity()));
        }
    }

    private void requireUser(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException("Invalid user");
        }
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

    private LimitOrderResponse toResponse(LimitOrder order) {
        return new LimitOrderResponse(
                order.getId(),
                order.getSymbol(),
                order.getSide(),
                order.getStatus(),
                order.getQuantity(),
                order.getLimitPrice(),
                order.getExecutionPrice(),
                order.getQuoteAmount(),
                order.getCreatedAt(),
                order.getFilledAt(),
                order.getCancelledAt());
    }

    private record Wallets(Wallet base, Wallet quote) {}
}
