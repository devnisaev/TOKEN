package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.TaxSummaryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaxSummaryRecordRepository extends JpaRepository<TaxSummaryRecord, UUID> {

    Page<TaxSummaryRecord> findByRecipientInvestorId(UUID recipientInvestorId, Pageable pageable);
}
