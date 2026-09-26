package com.tokenrealty.valuation.repository;

import com.tokenrealty.valuation.entity.ValuationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ValuationRequestRepository extends JpaRepository<ValuationRequest, UUID> {

    List<ValuationRequest> findByBuildingIdOrderByCreatedAtDesc(UUID buildingId);
}
