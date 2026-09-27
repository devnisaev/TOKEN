package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.OperatorAlertRecord;
import com.tokenrealty.reporting.entity.OperatorAlertRecord.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OperatorAlertRecordRepository extends JpaRepository<OperatorAlertRecord, UUID> {

    Optional<OperatorAlertRecord> findByDedupeKey(String dedupeKey);

    Page<OperatorAlertRecord> findByStatusOrderByDetectedAtDesc(AlertStatus status, Pageable pageable);

    long countByStatus(AlertStatus status);
}
