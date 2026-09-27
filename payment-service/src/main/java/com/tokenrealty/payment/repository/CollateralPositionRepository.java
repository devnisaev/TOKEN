package com.tokenrealty.payment.repository;

import com.tokenrealty.payment.entity.CollateralPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CollateralPositionRepository extends JpaRepository<CollateralPosition, UUID> {

    List<CollateralPosition> findByInvestorIdAndStatus(
            UUID investorId, CollateralPosition.CollateralStatus status);
}
