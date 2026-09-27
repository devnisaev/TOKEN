package com.tokenrealty.registry.repository;

import com.tokenrealty.registry.entity.InsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, UUID> {

    List<InsurancePolicy> findByBuildingId(UUID buildingId);

    List<InsurancePolicy> findByFlatId(UUID flatId);
}
