package com.tokenrealty.audit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.audit.kafka.AuditKafkaEventTypes;
import com.tokenrealty.audit.repository.AuditEntryRepository;
import com.tokenrealty.audit.service.AuditLedgerService;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
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
class AuditKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired AuditLedgerService auditLedgerService;
    @Autowired AuditEntryRepository repository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("kyc-approved creates immutable audit entry")
    void kycApproved_recorded() throws Exception {
        UUID investorId = UUID.randomUUID();
        publish(AuditKafkaEventTypes.KYC_APPROVED, UUID.randomUUID(), Map.of(
                "investorId", investorId.toString(),
                "walletAddress", "0xabc",
                "approvedAt", "2025-09-25T16:00:00Z"
        ), auditLedgerService::onKycApproved);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(auditLedgerService.findByInvestor(investorId, org.springframework.data.domain.Pageable.unpaged())
                .getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("duplicate eventId is deduped")
    void duplicateEventId_deduped() throws Exception {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "investorId", UUID.randomUUID().toString(),
                "walletAddress", "0xabc",
                "approvedAt", "2025-09-25T16:00:00Z"
        );
        publish(AuditKafkaEventTypes.KYC_APPROVED, eventId, payload, auditLedgerService::onKycApproved);
        publish(AuditKafkaEventTypes.KYC_APPROVED, eventId, payload, auditLedgerService::onKycApproved);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("settlement.recovered creates ORDER audit entry")
    void settlementRecovered_recorded() throws Exception {
        UUID orderId = UUID.randomUUID();
        publish(AuditKafkaEventTypes.SETTLEMENT_RECOVERED, UUID.randomUUID(), Map.of(
                "sagaId", UUID.randomUUID().toString(),
                "orderId", orderId.toString(),
                "currentStep", "AWAITING_TRANSFER",
                "recoveredAt", "2025-09-25T17:00:00Z"
        ), auditLedgerService::onSettlementRecovered);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findAll().getFirst().getSubjectId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("valuation.approved creates FLAT audit entry")
    void valuationApproved_recorded() throws Exception {
        UUID flatId = UUID.randomUUID();
        publish(AuditKafkaEventTypes.VALUATION_APPROVED, UUID.randomUUID(), Map.of(
                "flatId", flatId.toString(),
                "buildingId", UUID.randomUUID().toString(),
                "valuationRequestId", UUID.randomUUID().toString(),
                "valueUsd", "1000000.00",
                "totalTokens", 1000,
                "navPerTokenUsd", "1000.00000000",
                "approvedAt", "2025-09-25T18:00:00Z",
                "reviewedBy", UUID.randomUUID().toString()
        ), auditLedgerService::onValuationApproved);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(auditLedgerService.findByFlat(flatId, org.springframework.data.domain.Pageable.unpaged())
                .getTotalElements()).isEqualTo(1);
    }

    private void publish(String eventType, UUID eventId, Map<String, Object> payload,
                         java.util.function.Consumer<com.tokenrealty.events.kafka.KafkaJsonEvent> handler) throws Exception {
        EventEnvelope<Map<String, Object>> envelope = new EventEnvelope<>(
                eventId, eventType, Instant.parse("2025-09-25T16:00:00Z"), null, payload);
        eventConsumer.consume(objectMapper.writeValueAsString(envelope), eventType, "test", handler);
    }
}
