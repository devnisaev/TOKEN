package com.tokenrealty.marketplace.repository;

import com.tokenrealty.marketplace.entity.LiquidityPool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiquidityPoolRepository extends JpaRepository<LiquidityPool, UUID> {

    Optional<LiquidityPool> findByContractId(UUID contractId);

    List<LiquidityPool> findByFlatId(UUID flatId);

    List<LiquidityPool> findByStatus(LiquidityPool.PoolStatus status);
}
