package com.tokenrealty.marketplace.repository;

import com.tokenrealty.marketplace.entity.LpPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LpPositionRepository extends JpaRepository<LpPosition, UUID> {

    Optional<LpPosition> findByPoolIdAndInvestorId(UUID poolId, UUID investorId);

    List<LpPosition> findByPoolId(UUID poolId);

    List<LpPosition> findByInvestorId(UUID investorId);
}
