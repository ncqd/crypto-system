package com.project.crypto.repository;

import com.project.crypto.domain.entity.AggregatedPrice;
import com.project.crypto.domain.enums.TradingPair;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AggregatedPriceRepository extends JpaRepository<AggregatedPrice, Long> {

    Optional<AggregatedPrice> findBySymbol(TradingPair symbol);
}
