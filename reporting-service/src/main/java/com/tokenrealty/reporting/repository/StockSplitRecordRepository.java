package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.StockSplitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockSplitRecordRepository extends JpaRepository<StockSplitRecord, UUID> {
}
