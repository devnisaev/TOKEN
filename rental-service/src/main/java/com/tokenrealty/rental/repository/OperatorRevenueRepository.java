package com.tokenrealty.rental.repository;

import com.tokenrealty.rental.entity.OperatorRevenueRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OperatorRevenueRepository extends JpaRepository<OperatorRevenueRecord, UUID> {

    List<OperatorRevenueRecord> findByFlatIdOrderByPeriodEndDesc(UUID flatId);
}
