package com.tokenrealty.reporting.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.reporting.kafka.ReportingKafkaEventTypes;
import com.tokenrealty.reporting.repository.TaxSummaryRecordRepository;
import com.tokenrealty.reporting.service.ReportingProjectionService;
import com.tokenrealty.reporting.service.ReportingQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Tax summary query integration test")
class TaxSummaryQueryIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired ReportingProjectionService projectionService;
    @Autowired ReportingQueryService queryService;
    @Autowired TaxSummaryRecordRepository taxSummaryRecordRepository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void clean() {
        taxSummaryRecordRepository.deleteAll();
    }

    @Test
    @DisplayName("taxSummaries query returns projected withholding records")
    void taxSummaries_returnsProjectedRecords() {
        UUID investorId = UUID.randomUUID();
        ingestPayoutCompleted(UUID.randomUUID(), investorId);

        var page = queryService.taxSummaries(investorId, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().grossAmountUsd()).isEqualByComparingTo("100.00");
        assertThat(page.getContent().getFirst().withholdingAmountUsd()).isEqualByComparingTo("15.00");
        assertThat(page.getContent().getFirst().netAmountUsd()).isEqualByComparingTo("85.00");
    }

    private void ingestPayoutCompleted(UUID eventId, UUID investorId) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("payoutId", UUID.randomUUID().toString());
            payload.put("recipientInvestorId", investorId.toString());
            payload.put("grossAmountUsd", "100.00");
            payload.put("withholdingAmountUsd", "15.00");
            payload.put("netAmountUsd", "85.00");
            payload.put("completedAt", "2025-09-25T21:00:00Z");

            String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                    eventId,
                    ReportingKafkaEventTypes.PAYOUT_COMPLETED,
                    "test-trace",
                    payload));
            eventConsumer.consume(message, ReportingKafkaEventTypes.PAYOUT_COMPLETED,
                    "Tax summary projection failed",
                    projectionService::onPayoutCompleted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
