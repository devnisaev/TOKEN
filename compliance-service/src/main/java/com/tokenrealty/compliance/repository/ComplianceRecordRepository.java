package com.tokenrealty.compliance.repository;

import com.tokenrealty.compliance.entity.ComplianceRecord;
import com.tokenrealty.compliance.entity.ComplianceRecord.ComplianceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplianceRecordRepository extends JpaRepository<ComplianceRecord, UUID> {

    Optional<ComplianceRecord> findByWalletAddress(String walletAddress);

    Optional<ComplianceRecord> findByInvestorId(UUID investorId);

    Optional<ComplianceRecord> findByKycReferenceId(String kycReferenceId);

    boolean existsByWalletAddress(String walletAddress);

    boolean existsByInvestorId(UUID investorId);

    List<ComplianceRecord> findByStatusAndKycExpiresAtBefore(ComplianceStatus status, Instant expiresBefore);

    Page<ComplianceRecord> findByStatus(ComplianceStatus status, Pageable pageable);
}
