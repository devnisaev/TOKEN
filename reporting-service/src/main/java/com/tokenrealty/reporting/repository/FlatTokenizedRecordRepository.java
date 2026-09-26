package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.FlatTokenizedRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FlatTokenizedRecordRepository extends JpaRepository<FlatTokenizedRecord, UUID> {
}
