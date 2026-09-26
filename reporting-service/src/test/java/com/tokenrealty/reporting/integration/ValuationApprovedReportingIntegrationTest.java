package com.tokenrealty.reporting.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.reporting.kafka.ReportingKafkaEventTypes;
import com.tokenrealty.reporting.repository.ValuationApprovedRecordRepository;
import com.tokenrealty.reporting.service.ReportingProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Reporting valuation.approved Kafka integration test")
class ValuationApprovedReportingIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired ReportingProjectionService projectionService;
    @Autowired ValuationApprovedRecordRepository valuationApprovedRecordRepository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void cleanProjections() {
        valuationApprovedRecordRepository.deleteAll();
    }

    @Test
    @DisplayName("valuation.approved creates ValuationApprovedRecord projection")
    void valuationApproved_createsProjection() throws Exception {
        UUID flatId = UUID.randomUUID();
        UUID buildingId = UUID.randomUUID();
        ingestValuationApproved(flatId, buildingId, UUID.randomUUID());

        var record = valuationApprovedRecordRepository.findAll().getFirst();
        assertThat(record.getFlatId()).isEqualTo(flatId);
        assertThat(record.getBuildingId()).isEqualTo(buildingId);
        assertThat(record.getValueUsd()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(record.getNavPerTokenUsd()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(record.getApprovedAt()).isEqualTo(Instant.parse("2025-09-25T18:00:00Z"));
    }

    private void ingestValuationApproved(UUID flatId, UUID buildingId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("flatId", flatId.toString());
        payload.put("buildingId", buildingId.toString());
        payload.put("valuationRequestId", UUID.randomUUID().toString());
        payload.put("valueUsd", "1000000.00");
        payload.put("totalTokens", 1000);
        payload.put("navPerTokenUsd", "1000.00");
        payload.put("approvedAt", "2025-09-25T18:00:00Z");
        payload.put("reviewedBy", UUID.randomUUID().toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                ReportingKafkaEventTypes.VALUATION_APPROVED,
                "test-trace",
                payload));
        eventConsumer.consume(message, ReportingKafkaEventTypes.VALUATION_APPROVED,
                "Valuation approved projection failed",
                projectionService::onValuationApproved);
    }
}
