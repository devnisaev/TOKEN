package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.TradeSettledRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TradeSettledRecordRepository extends JpaRepository<TradeSettledRecord, UUID> {
}
