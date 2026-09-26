package com.tokenrealty.reporting.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.reporting.kafka.ReportingKafkaEventTypes;
import com.tokenrealty.reporting.repository.StuckSagaRecordRepository;
import com.tokenrealty.reporting.service.ReportingProjectionService;
import org.junit.jupiter.api.BeforeEach;
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
@DisplayName("Reporting settlement.stuck Kafka integration test")
class SettlementStuckReportingIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired ReportingProjectionService projectionService;
    @Autowired StuckSagaRecordRepository stuckSagaRecordRepository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void cleanProjections() {
        stuckSagaRecordRepository.deleteAll();
    }

    @Test
    @DisplayName("settlement.stuck creates StuckSagaRecord projection")
    void settlementStuck_createsProjection() throws Exception {
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        ingestSettlementStuck(sagaId, orderId, UUID.randomUUID());

        var record = stuckSagaRecordRepository.findAll().getFirst();
        assertThat(record.getSagaId()).isEqualTo(sagaId);
        assertThat(record.getOrderId()).isEqualTo(orderId);
        assertThat(record.getCurrentStep()).isEqualTo("AWAITING_TRANSFER");
        assertThat(record.getStuckAt()).isEqualTo(Instant.parse("2025-09-25T14:00:00Z"));
    }

    @Test
    @DisplayName("duplicate eventId is deduped")
    void duplicateEventId_deduped() throws Exception {
        UUID eventId = UUID.randomUUID();
        ingestSettlementStuck(UUID.randomUUID(), UUID.randomUUID(), eventId);
        ingestSettlementStuck(UUID.randomUUID(), UUID.randomUUID(), eventId);

        assertThat(stuckSagaRecordRepository.count()).isEqualTo(1);
    }

    private void ingestSettlementStuck(UUID sagaId, UUID orderId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sagaId", sagaId.toString());
        payload.put("orderId", orderId.toString());
        payload.put("currentStep", "AWAITING_TRANSFER");
        payload.put("stuckAt", "2025-09-25T14:00:00Z");

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                ReportingKafkaEventTypes.SETTLEMENT_STUCK,
                "test-trace",
                payload));
        eventConsumer.consume(message, ReportingKafkaEventTypes.SETTLEMENT_STUCK,
                "Settlement stuck projection failed",
                projectionService::onSettlementStuck);
    }
}
