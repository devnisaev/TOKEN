package com.tokenrealty.issuance.repository;

import com.tokenrealty.issuance.entity.ComplianceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceRecordRepository extends JpaRepository<ComplianceRecord, UUID> {

    Optional<ComplianceRecord> findByWalletAddress(String walletAddress);

    Optional<ComplianceRecord> findByInvestorId(UUID investorId);

    Page<ComplianceRecord> findByStatus(ComplianceRecord.ComplianceStatus status, Pageable pageable);

    boolean existsByWalletAddress(String walletAddress);

    boolean existsByInvestorId(UUID investorId);
}