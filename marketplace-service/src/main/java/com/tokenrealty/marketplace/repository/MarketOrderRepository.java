package com.tokenrealty.marketplace.repository;

import com.tokenrealty.marketplace.entity.MarketOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MarketOrderRepository extends JpaRepository<MarketOrder, UUID> {

    Page<MarketOrder> findByBuyerId(UUID buyerId, Pageable pageable);

    Page<MarketOrder> findByListingId(UUID listingId, Pageable pageable);

    Page<MarketOrder> findByStatus(MarketOrder.OrderStatus status, Pageable pageable);

    Optional<MarketOrder> findFirstByListingIdAndOrderTypeAndStatus(
            UUID listingId, MarketOrder.OrderType orderType, MarketOrder.OrderStatus status);
}
