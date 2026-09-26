package com.tokenrealty.reporting.repository;

import com.tokenrealty.reporting.entity.OrderMatchedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.UUID;

public interface OrderMatchedRecordRepository extends JpaRepository<OrderMatchedRecord, UUID> {

    @Query("select coalesce(sum(o.totalPriceUsd), 0) from OrderMatchedRecord o")
    BigDecimal sumTotalMatchedVolumeUsd();
}
