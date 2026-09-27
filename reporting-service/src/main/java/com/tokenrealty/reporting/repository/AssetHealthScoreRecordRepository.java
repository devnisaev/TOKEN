package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.AssetHealthScoreRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetHealthScoreRecordRepository extends JpaRepository<AssetHealthScoreRecord, UUID> {

    List<AssetHealthScoreRecord> findByBuildingIdOrderByComputedAtDesc(UUID buildingId);

    List<AssetHealthScoreRecord> findAllByOrderByComputedAtDesc();

    List<AssetHealthScoreRecord> findByFlatIdIn(java.util.Collection<UUID> flatIds);

    List<AssetHealthScoreRecord> findByFlatIdOrderByComputedAtDesc(UUID flatId);
}
