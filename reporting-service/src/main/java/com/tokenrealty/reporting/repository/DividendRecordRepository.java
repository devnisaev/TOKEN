package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.DividendRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DividendRecordRepository extends JpaRepository<DividendRecord, UUID> {

    List<DividendRecord> findAllByOrderByDistributedAtDesc();
}
