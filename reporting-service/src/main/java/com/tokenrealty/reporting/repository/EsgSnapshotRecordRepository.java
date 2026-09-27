package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.EsgSnapshotRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EsgSnapshotRecordRepository extends JpaRepository<EsgSnapshotRecord, UUID> {

    List<EsgSnapshotRecord> findByBuildingIdOrderBySnapshotAtDesc(UUID buildingId);

    List<EsgSnapshotRecord> findByFlatIdOrderBySnapshotAtDesc(UUID flatId);
}
