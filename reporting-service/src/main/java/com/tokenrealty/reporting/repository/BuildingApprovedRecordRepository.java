package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.BuildingApprovedRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BuildingApprovedRecordRepository extends JpaRepository<BuildingApprovedRecord, UUID> {
}
