package com.tokenrealty.reporting.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.reporting.kafka.ReportingKafkaEventTypes;
import com.tokenrealty.reporting.repository.TaxSummaryRecordRepository;
import com.tokenrealty.reporting.service.ReportingProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TaxSummaryReportingIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired ReportingProjectionService projectionService;
    @Autowired TaxSummaryRecordRepository taxSummaryRecordRepository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void clean() {
        taxSummaryRecordRepository.deleteAll();
    }

    @Test
    void payoutCompletedWithWithholding_createsTaxSummaryRecord() throws Exception {
        UUID payoutId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        ingest(payoutId, recipientId);

        var record = taxSummaryRecordRepository.findAll().getFirst();
        assertThat(record.getPayoutId()).isEqualTo(payoutId);
        assertThat(record.getWithholdingAmountUsd()).isEqualByComparingTo("15.00");
    }

    private void ingest(UUID payoutId, UUID recipientId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("payoutId", payoutId.toString());
        payload.put("recipientInvestorId", recipientId.toString());
        payload.put("grossAmountUsd", "100.00");
        payload.put("withholdingAmountUsd", "15.00");
        payload.put("netAmountUsd", "85.00");
        payload.put("completedAt", "2025-09-25T21:00:00Z");

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                UUID.randomUUID(),
                ReportingKafkaEventTypes.PAYOUT_COMPLETED,
                "test-trace",
                payload));
        eventConsumer.consume(message, ReportingKafkaEventTypes.PAYOUT_COMPLETED,
                "Tax summary projection failed",
                projectionService::onPayoutCompleted);
    }
}
