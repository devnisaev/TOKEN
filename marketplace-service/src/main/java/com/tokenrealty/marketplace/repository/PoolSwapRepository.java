package com.tokenrealty.marketplace.repository;

import com.tokenrealty.marketplace.entity.PoolSwap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PoolSwapRepository extends JpaRepository<PoolSwap, UUID> {

    Page<PoolSwap> findByPoolIdOrderByCreatedAtDesc(UUID poolId, Pageable pageable);
}
