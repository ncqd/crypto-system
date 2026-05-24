package com.project.crypto.repository;

import com.project.crypto.domain.entity.LimitOrder;
import com.project.crypto.domain.enums.OrderStatus;
import com.project.crypto.domain.enums.TradingPair;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LimitOrderRepository extends JpaRepository<LimitOrder, Long> {

    List<LimitOrder> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    @Query("SELECT o.id FROM LimitOrder o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<Long> findIdsByStatusOrderByCreatedAtAsc(@Param("status") OrderStatus status);

    List<LimitOrder> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status);

    List<LimitOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM LimitOrder o WHERE o.id = :id AND o.user.id = :userId")
    Optional<LimitOrder> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM LimitOrder o JOIN FETCH o.user WHERE o.id = :id AND o.status = :status")
    Optional<LimitOrder> findByIdAndStatusForUpdate(@Param("id") Long id, @Param("status") OrderStatus status);

    List<LimitOrder> findByStatusAndSymbolOrderByCreatedAtAsc(OrderStatus status, TradingPair symbol);
}
