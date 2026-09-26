package com.tokenrealty.settlement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tokenrealty.events.kafka.KafkaJsonEvent;
import com.tokenrealty.settlement.dto.SettlementDtos.SagaStepView;
import com.tokenrealty.settlement.dto.SettlementDtos.SettlementRetryResponse;
import com.tokenrealty.settlement.dto.SettlementDtos.SettlementSagaView;
import com.tokenrealty.settlement.entity.SagaStatus;
import com.tokenrealty.settlement.entity.SagaStepName;
import com.tokenrealty.settlement.entity.SettlementSaga;
import com.tokenrealty.settlement.entity.SettlementSagaStep;
import com.tokenrealty.settlement.repository.SettlementSagaRepository;
import com.tokenrealty.settlement.repository.SettlementSagaStepRepository;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettlementSagaService {

    private final SettlementSagaRepository sagaRepository;
    private final SettlementSagaStepRepository stepRepository;
    private final Clock clock;

    @Transactional
    public void onOrderMatched(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        UUID orderId = uuid(payload, "orderId");
        SettlementSaga saga = sagaRepository.findByOrderId(orderId).orElseGet(() ->
                sagaRepository.save(SettlementSaga.builder()
                        .orderId(orderId)
                        .listingId(uuid(payload, "listingId"))
                        .flatId(uuid(payload, "flatId"))
                        .status(SagaStatus.IN_PROGRESS)
                        .currentStep(SagaStepName.PAYMENT_CONFIRMED)
                        .startedAt(event.occurredAt())
                        .build()));
        completeStep(saga, SagaStepName.ORDER_MATCHED, event);
    }

    @Transactional
    public void onPaymentConfirmed(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        SettlementSaga saga = findOrCreateSaga(uuid(payload, "orderId"), event.occurredAt());
        saga.setPaymentId(uuid(payload, "paymentId"));
        saga.setCurrentStep(SagaStepName.TRANSFER_COMPLETED);
        completeStep(saga, SagaStepName.PAYMENT_CONFIRMED, event);
    }

    @Transactional
    public void onTransferCompleted(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        SettlementSaga saga = findOrCreateSaga(uuid(payload, "orderId"), event.occurredAt());
        saga.setTransferId(uuid(payload, "transferId"));
        saga.setFlatId(firstNonNull(saga.getFlatId(), uuid(payload, "flatId")));
        saga.setCurrentStep(SagaStepName.TRADE_SETTLED);
        completeStep(saga, SagaStepName.TRANSFER_COMPLETED, event);
    }

    @Transactional
    public void onTradeSettled(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        SettlementSaga saga = findOrCreateSaga(uuid(payload, "orderId"), event.occurredAt());
        saga.setTradeId(uuid(payload, "tradeId"));
        saga.setPaymentId(firstNonNull(saga.getPaymentId(), uuid(payload, "paymentId")));
        saga.setTransferId(firstNonNull(saga.getTransferId(), uuid(payload, "transferId")));
        saga.setListingId(firstNonNull(saga.getListingId(), uuid(payload, "listingId")));
        completeStep(saga, SagaStepName.TRADE_SETTLED, event);
        saga.setStatus(SagaStatus.COMPLETED);
        saga.setCurrentStep(SagaStepName.TRADE_SETTLED);
        saga.setCompletedAt(event.occurredAt());
    }

    @Transactional(readOnly = true)
    public SettlementSagaView getByOrderId(UUID orderId) {
        return toView(requireSaga(orderId));
    }

    @Transactional
    public SettlementRetryResponse retry(UUID orderId) {
        SettlementSaga saga = requireSaga(orderId);
        if (saga.getStatus() != SagaStatus.STUCK) {
            return new SettlementRetryResponse(orderId, saga.getStatus(),
                    "Retry only applies to STUCK sagas");
        }
        saga.setStatus(SagaStatus.IN_PROGRESS);
        sagaRepository.save(saga);
        return new SettlementRetryResponse(orderId, SagaStatus.IN_PROGRESS,
                "Saga marked IN_PROGRESS for ops follow-up");
    }

    @Transactional
    public int markStuckSagas(Instant updatedBefore) {
        int count = 0;
        for (SettlementSaga saga : sagaRepository.findByStatusAndUpdatedAtBefore(
                SagaStatus.IN_PROGRESS, updatedBefore)) {
            saga.setStatus(SagaStatus.STUCK);
            sagaRepository.save(saga);
            count++;
        }
        return count;
    }

    private void completeStep(SettlementSaga saga, SagaStepName stepName, KafkaJsonEvent event) {
        boolean exists = saga.getSteps().stream().anyMatch(step -> step.getStepName() == stepName);
        if (exists) {
            return;
        }
        SettlementSagaStep step = stepRepository.save(SettlementSagaStep.builder()
                .saga(saga)
                .stepName(stepName)
                .sourceEventId(event.eventId())
                .completedAt(event.occurredAt())
                .build());
        saga.getSteps().add(step);
        sagaRepository.save(saga);
    }

    private SettlementSaga findOrCreateSaga(UUID orderId, Instant startedAt) {
        return sagaRepository.findByOrderId(orderId).orElseGet(() ->
                sagaRepository.save(SettlementSaga.builder()
                        .orderId(orderId)
                        .status(SagaStatus.IN_PROGRESS)
                        .currentStep(SagaStepName.ORDER_MATCHED)
                        .startedAt(startedAt)
                        .build()));
    }

    private SettlementSaga requireSaga(UUID orderId) {
        return sagaRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement saga not found for order " + orderId));
    }

    private SettlementSagaView toView(SettlementSaga saga) {
        var steps = saga.getSteps().stream()
                .sorted(Comparator.comparing(SettlementSagaStep::getCompletedAt))
                .map(step -> new SagaStepView(step.getStepName(), step.getCompletedAt(), step.getSourceEventId()))
                .toList();
        return new SettlementSagaView(
                saga.getId(),
                saga.getOrderId(),
                saga.getTradeId(),
                saga.getPaymentId(),
                saga.getTransferId(),
                saga.getListingId(),
                saga.getFlatId(),
                saga.getStatus(),
                saga.getCurrentStep(),
                saga.getStartedAt(),
                saga.getCompletedAt(),
                saga.getUpdatedAt(),
                steps
        );
    }

    private static UUID uuid(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return UUID.fromString(value.asText());
    }

    private static UUID firstNonNull(UUID primary, UUID fallback) {
        return primary != null ? primary : fallback;
    }
}
