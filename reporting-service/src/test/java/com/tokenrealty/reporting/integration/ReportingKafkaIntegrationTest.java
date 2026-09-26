package com.tokenrealty.reporting.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.reporting.kafka.ReportingKafkaEventTypes;
import com.tokenrealty.reporting.repository.OrderMatchedRecordRepository;
import com.tokenrealty.reporting.repository.TradeSettledRecordRepository;
import com.tokenrealty.reporting.service.ReportingProjectionService;
import com.tokenrealty.reporting.service.ReportingQueryService;
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
@DisplayName("Reporting Kafka integration test")
class ReportingKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired ReportingProjectionService projectionService;
    @Autowired ReportingQueryService queryService;
    @Autowired TradeSettledRecordRepository tradeSettledRecordRepository;
    @Autowired OrderMatchedRecordRepository orderMatchedRecordRepository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void cleanProjections() {
        tradeSettledRecordRepository.deleteAll();
        orderMatchedRecordRepository.deleteAll();
    }

    @Test
    @DisplayName("order.matched and trade.settled update trading summary")
    void tradingSummary_afterEvents() throws Exception {
        UUID orderId = UUID.randomUUID();
        ingestOrderMatched(orderId, UUID.randomUUID());
        ingestTradeSettled(orderId, UUID.randomUUID(), UUID.randomUUID());

        var summary = queryService.tradingSummary();
        assertThat(summary.settledTradeCount()).isEqualTo(1);
        assertThat(summary.matchedOrderCount()).isEqualTo(1);
        assertThat(summary.matchedVolumeUsd()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("duplicate eventId is deduped")
    void duplicateEventId_deduped() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        ingestOrderMatched(orderId, eventId);
        ingestOrderMatched(orderId, eventId);

        assertThat(orderMatchedRecordRepository.count()).isEqualTo(1);
    }

    private void ingestOrderMatched(UUID orderId, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId.toString());
        payload.put("listingId", UUID.randomUUID().toString());
        payload.put("flatId", UUID.randomUUID().toString());
        payload.put("contractId", UUID.randomUUID().toString());
        payload.put("buyerId", UUID.randomUUID().toString());
        payload.put("sellerId", UUID.randomUUID().toString());
        payload.put("tokenAmount", 100);
        payload.put("totalPriceUsd", "10000.00");

        publish(ReportingKafkaEventTypes.ORDER_MATCHED, eventId, payload);
    }

    private void ingestTradeSettled(UUID orderId, UUID eventId, UUID tradeId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tradeId", tradeId.toString());
        payload.put("orderId", orderId.toString());
        payload.put("listingId", UUID.randomUUID().toString());
        payload.put("paymentId", UUID.randomUUID().toString());
        payload.put("transferId", UUID.randomUUID().toString());

        publish(ReportingKafkaEventTypes.TRADE_SETTLED, eventId, payload);
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
        if (ReportingKafkaEventTypes.TRADE_SETTLED.equals(eventType)) {
            eventConsumer.consume(message, eventType, "Reporting test failed",
                    projectionService::onTradeSettled);
        } else {
            eventConsumer.consume(message, eventType, "Reporting test failed",
                    projectionService::onOrderMatched);
        }
    }
}
