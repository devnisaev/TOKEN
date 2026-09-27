package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.RentCollectedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.UUID;

public interface RentCollectedRecordRepository extends JpaRepository<RentCollectedRecord, UUID> {

    @Query("select count(distinct r.flatId) from RentCollectedRecord r where r.flatId is not null")
    long countDistinctOccupiedFlats();

    @Query("select coalesce(sum(r.amountUsd), 0) from RentCollectedRecord r")
    BigDecimal sumCollectedUsd();
}
