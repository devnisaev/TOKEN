package com.tokenrealty.marketplace.repository;

import com.tokenrealty.marketplace.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {

    Optional<Trade> findByOrderId(UUID orderId);
}
