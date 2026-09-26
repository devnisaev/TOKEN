package com.tokenrealty.settlement.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.settlement.entity.SagaStatus;
import com.tokenrealty.settlement.entity.SagaStepName;
import com.tokenrealty.settlement.kafka.SettlementKafkaEventTypes;
import com.tokenrealty.settlement.service.SettlementSagaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Settlement Kafka integration test")
class SettlementKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired SettlementSagaService sagaService;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("buy flow events produce completed saga timeline")
    void fullBuyFlow_completesSaga() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID tradeId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();

        publish(SettlementKafkaEventTypes.ORDER_MATCHED, UUID.randomUUID(), orderMatchedPayload(orderId));
        publish(SettlementKafkaEventTypes.PAYMENT_CONFIRMED, UUID.randomUUID(),
                paymentConfirmedPayload(orderId, paymentId));
        publish(SettlementKafkaEventTypes.TRANSFER_COMPLETED, UUID.randomUUID(),
                transferCompletedPayload(orderId, transferId));
        publish(SettlementKafkaEventTypes.TRADE_SETTLED, UUID.randomUUID(),
                tradeSettledPayload(orderId, tradeId, paymentId, transferId));

        var view = sagaService.getByOrderId(orderId);
        assertThat(view.status()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(view.currentStep()).isEqualTo(SagaStepName.TRADE_SETTLED);
        assertThat(view.steps()).hasSize(4);
        assertThat(view.tradeId()).isEqualTo(tradeId);
        assertThat(view.paymentId()).isEqualTo(paymentId);
        assertThat(view.transferId()).isEqualTo(transferId);
    }

    @Test
    @DisplayName("admin retry clears STUCK status")
    void retry_marksStuckSagaInProgress() throws Exception {
        UUID orderId = UUID.randomUUID();
        publish(SettlementKafkaEventTypes.ORDER_MATCHED, UUID.randomUUID(), orderMatchedPayload(orderId));

        sagaService.markStuckSagas(Instant.now().plusSeconds(60));
        var retry = sagaService.retry(orderId);
        assertThat(retry.status()).isEqualTo(SagaStatus.IN_PROGRESS);
    }

    private Map<String, Object> orderMatchedPayload(UUID orderId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId.toString());
        payload.put("listingId", UUID.randomUUID().toString());
        payload.put("flatId", UUID.randomUUID().toString());
        payload.put("contractId", UUID.randomUUID().toString());
        payload.put("buyerId", UUID.randomUUID().toString());
        payload.put("sellerId", UUID.randomUUID().toString());
        payload.put("tokenAmount", 100);
        payload.put("totalPriceUsd", "10000.00");
        return payload;
    }

    private Map<String, Object> paymentConfirmedPayload(UUID orderId, UUID paymentId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", paymentId.toString());
        payload.put("orderId", orderId.toString());
        payload.put("payerId", UUID.randomUUID().toString());
        payload.put("confirmedAt", "2025-09-25T16:01:00Z");
        return payload;
    }

    private Map<String, Object> transferCompletedPayload(UUID orderId, UUID transferId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transferId", transferId.toString());
        payload.put("orderId", orderId.toString());
        payload.put("flatId", UUID.randomUUID().toString());
        payload.put("completedAt", "2025-09-25T16:02:00Z");
        return payload;
    }

    private Map<String, Object> tradeSettledPayload(UUID orderId, UUID tradeId, UUID paymentId, UUID transferId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tradeId", tradeId.toString());
        payload.put("orderId", orderId.toString());
        payload.put("listingId", UUID.randomUUID().toString());
        payload.put("paymentId", paymentId.toString());
        payload.put("transferId", transferId.toString());
        return payload;
    }

    private void publish(String eventType, UUID eventId, Map<String, Object> payload) throws Exception {
        EventEnvelope envelope = new EventEnvelope(
                eventId,
                eventType,
                Instant.parse("2025-09-25T16:00:00Z"),
                null,
                payload
        );
        String message = objectMapper.writeValueAsString(envelope);
        switch (eventType) {
            case SettlementKafkaEventTypes.ORDER_MATCHED ->
                    eventConsumer.consume(message, eventType, "test", sagaService::onOrderMatched);
            case SettlementKafkaEventTypes.PAYMENT_CONFIRMED ->
                    eventConsumer.consume(message, eventType, "test", sagaService::onPaymentConfirmed);
            case SettlementKafkaEventTypes.TRANSFER_COMPLETED ->
                    eventConsumer.consume(message, eventType, "test", sagaService::onTransferCompleted);
            case SettlementKafkaEventTypes.TRADE_SETTLED ->
                    eventConsumer.consume(message, eventType, "test", sagaService::onTradeSettled);
            default -> throw new IllegalArgumentException(eventType);
        }
    }
}
