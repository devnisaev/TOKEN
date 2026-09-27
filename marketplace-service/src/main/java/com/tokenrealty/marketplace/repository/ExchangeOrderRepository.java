package com.tokenrealty.marketplace.repository;

import com.tokenrealty.marketplace.entity.ExchangeOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeOrderRepository extends JpaRepository<ExchangeOrder, UUID> {

    Page<ExchangeOrder> findByInvestorIdAndStatusIn(
            UUID investorId,
            List<ExchangeOrder.OrderStatus> statuses,
            Pageable pageable);

    @Query("""
            SELECT o FROM ExchangeOrder o
            WHERE o.contractId = :contractId
              AND o.side = com.tokenrealty.marketplace.entity.ExchangeOrder$OrderSide.ASK
              AND o.status IN (com.tokenrealty.marketplace.entity.ExchangeOrder$OrderStatus.OPEN,
                               com.tokenrealty.marketplace.entity.ExchangeOrder$OrderStatus.PARTIALLY_FILLED)
              AND o.limitPriceUsd <= :maxPrice
            ORDER BY o.limitPriceUsd ASC, o.createdAt ASC
            """)
    List<ExchangeOrder> findMatchingAsks(
            @Param("contractId") UUID contractId,
            @Param("maxPrice") java.math.BigDecimal maxPrice);

    @Query("""
            SELECT o FROM ExchangeOrder o
            WHERE o.contractId = :contractId
              AND o.side = com.tokenrealty.marketplace.entity.ExchangeOrder$OrderSide.BID
              AND o.status IN (com.tokenrealty.marketplace.entity.ExchangeOrder$OrderStatus.OPEN,
                               com.tokenrealty.marketplace.entity.ExchangeOrder$OrderStatus.PARTIALLY_FILLED)
              AND o.limitPriceUsd >= :minPrice
            ORDER BY o.limitPriceUsd DESC, o.createdAt ASC
            """)
    List<ExchangeOrder> findMatchingBids(
            @Param("contractId") UUID contractId,
            @Param("minPrice") java.math.BigDecimal minPrice);

    @Query("""
            SELECT o FROM ExchangeOrder o
            WHERE o.contractId = :contractId
              AND o.status IN (com.tokenrealty.marketplace.entity.ExchangeOrder$OrderStatus.OPEN,
                               com.tokenrealty.marketplace.entity.ExchangeOrder$OrderStatus.PARTIALLY_FILLED)
            ORDER BY o.side ASC, o.limitPriceUsd DESC, o.createdAt ASC
            """)
    List<ExchangeOrder> findOpenBook(@Param("contractId") UUID contractId);

    Optional<ExchangeOrder> findByIdAndInvestorId(UUID id, UUID investorId);
}
