package com.tokenrealty.settlement.dto;

import com.tokenrealty.settlement.entity.SagaStatus;
import com.tokenrealty.settlement.entity.SagaStepName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SettlementDtos {

    private SettlementDtos() {
    }

    public record SagaStepView(
            SagaStepName stepName,
            Instant completedAt,
            UUID sourceEventId
    ) {
    }

    public record SettlementSagaView(
            UUID id,
            UUID orderId,
            UUID tradeId,
            UUID paymentId,
            UUID transferId,
            UUID listingId,
            UUID flatId,
            SagaStatus status,
            SagaStepName currentStep,
            Instant startedAt,
            Instant completedAt,
            Instant updatedAt,
            List<SagaStepView> steps
    ) {
    }

    public record SettlementRetryResponse(
            UUID orderId,
            SagaStatus status,
            String message
    ) {
    }
}
