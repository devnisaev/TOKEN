package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.ValuationApprovedRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ValuationApprovedRecordRepository extends JpaRepository<ValuationApprovedRecord, UUID> {
}
