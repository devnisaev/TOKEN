package com.tokenrealty.reporting.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.reporting.kafka.ReportingKafkaEventTypes;
import com.tokenrealty.reporting.repository.StockSplitRecordRepository;
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
class StockSplitCompletedReportingIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired ReportingProjectionService projectionService;
    @Autowired StockSplitRecordRepository stockSplitRecordRepository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void cleanProjections() {
        stockSplitRecordRepository.deleteAll();
    }

    @Test
    @DisplayName("stock-split.completed creates StockSplitRecord projection")
    void stockSplitCompleted_createsProjection() throws Exception {
        UUID corporateActionId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();
        ingest(corporateActionId, flatId, UUID.randomUUID());

        var record = stockSplitRecordRepository.findAll().getFirst();
        assertThat(record.getCorporateActionId()).isEqualTo(corporateActionId);
        assertThat(record.getFlatId()).isEqualTo(flatId);
        assertThat(record.getSplitRatio()).isEqualByComparingTo(new BigDecimal("2.0000"));
        assertThat(record.getNewTotalSupply()).isEqualTo(2000L);
        assertThat(record.getNewTokenPriceUsd()).isEqualByComparingTo("50.00");
        assertThat(record.getCompletedAt()).isEqualTo(Instant.parse("2025-09-25T20:00:00Z"));
    }

    private void ingest(UUID corporateActionId, UUID flatId, UUID contractId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("corporateActionId", corporateActionId.toString());
        payload.put("flatId", flatId.toString());
        payload.put("contractId", contractId.toString());
        payload.put("period", "2025-Q3");
        payload.put("splitRatio", "2.0000");
        payload.put("newTotalSupply", 2000);
        payload.put("newTokenPriceUsd", "50.00");
        payload.put("completedAt", "2025-09-25T20:00:00Z");

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                UUID.randomUUID(),
                ReportingKafkaEventTypes.STOCK_SPLIT_COMPLETED,
                "test-trace",
                payload));
        eventConsumer.consume(message, ReportingKafkaEventTypes.STOCK_SPLIT_COMPLETED,
                "Stock split completed projection failed",
                projectionService::onStockSplitCompleted);
    }
}
