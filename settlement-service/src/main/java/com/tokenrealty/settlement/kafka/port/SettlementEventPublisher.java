package com.tokenrealty.settlement.kafka.port;

import com.tokenrealty.settlement.entity.SagaStepName;

import java.time.Instant;
import java.util.UUID;

public interface SettlementEventPublisher {

    void publishStuck(SettlementStuckEvent event);

    void publishRecovered(SettlementRecoveredEvent event);

    record SettlementStuckEvent(
            UUID sagaId,
            UUID orderId,
            SagaStepName currentStep,
            Instant stuckAt
    ) {
    }

    record SettlementRecoveredEvent(
            UUID sagaId,
            UUID orderId,
            SagaStepName currentStep,
            Instant recoveredAt
    ) {
    }
}
