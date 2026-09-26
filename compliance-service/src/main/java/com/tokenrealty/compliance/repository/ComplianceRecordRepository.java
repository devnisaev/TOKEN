package com.tokenrealty.compliance.repository;

import com.tokenrealty.compliance.entity.ComplianceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ComplianceRecordRepository extends JpaRepository<ComplianceRecord, UUID> {

    Optional<ComplianceRecord> findByWalletAddress(String walletAddress);

    Optional<ComplianceRecord> findByInvestorId(UUID investorId);

    boolean existsByWalletAddress(String walletAddress);

    boolean existsByInvestorId(UUID investorId);
}
