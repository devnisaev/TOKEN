package com.tokenrealty.settlement.repository;

import com.tokenrealty.settlement.entity.SagaStatus;
import com.tokenrealty.settlement.entity.SettlementSaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementSagaRepository extends JpaRepository<SettlementSaga, UUID> {

    Optional<SettlementSaga> findByOrderId(UUID orderId);

    List<SettlementSaga> findByStatusAndUpdatedAtBefore(SagaStatus status, Instant threshold);
}
