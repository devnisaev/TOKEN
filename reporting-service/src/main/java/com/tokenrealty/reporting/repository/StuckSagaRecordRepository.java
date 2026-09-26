package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.StuckSagaRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StuckSagaRecordRepository extends JpaRepository<StuckSagaRecord, UUID> {
}
