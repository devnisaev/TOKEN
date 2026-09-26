package com.tokenrealty.compliance.repository;

import com.tokenrealty.compliance.entity.InvestmentPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvestmentPolicyRepository extends JpaRepository<InvestmentPolicy, UUID> {

    Optional<InvestmentPolicy> findByJurisdiction(String jurisdiction);
}
