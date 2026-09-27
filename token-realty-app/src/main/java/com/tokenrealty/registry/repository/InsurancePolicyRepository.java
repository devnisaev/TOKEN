package com.tokenrealty.registry.repository;

import com.tokenrealty.registry.entity.InsurancePolicy;
import com.tokenrealty.registry.entity.InsurancePolicy.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, UUID> {

    List<InsurancePolicy> findByBuildingId(UUID buildingId);

    List<InsurancePolicy> findByFlatId(UUID flatId);

    List<InsurancePolicy> findByStatusAndExpiresAtBetween(
            PolicyStatus status, Instant expiresAfter, Instant expiresBefore);
}
