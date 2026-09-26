package com.tokenrealty.settlement.repository;

import com.tokenrealty.settlement.entity.SettlementSagaStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SettlementSagaStepRepository extends JpaRepository<SettlementSagaStep, UUID> {
}
