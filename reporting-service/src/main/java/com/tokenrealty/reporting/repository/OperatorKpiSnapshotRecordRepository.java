package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.OperatorKpiSnapshotRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OperatorKpiSnapshotRecordRepository extends JpaRepository<OperatorKpiSnapshotRecord, UUID> {

    List<OperatorKpiSnapshotRecord> findTop30ByOrderBySnapshotAtDesc();
}
