package com.tokenrealty.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tokenrealty.audit.entity.AuditEntry;
import com.tokenrealty.audit.entity.AuditSubjectType;
import com.tokenrealty.audit.repository.AuditEntryRepository;
import com.tokenrealty.events.kafka.KafkaJsonEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLedgerService {

    private final AuditEntryRepository repository;

    @Transactional
    public void record(KafkaJsonEvent event, AuditSubjectType subjectType, UUID subjectId,
                       UUID actorId, String summary) {
        repository.save(AuditEntry.builder()
                .sourceEventId(event.eventId())
                .eventType(event.eventType())
                .subjectType(subjectType)
                .subjectId(subjectId)
                .actorId(actorId)
                .summary(summary)
                .occurredAt(event.occurredAt())
                .build());
    }

    @Transactional
    public void onKycApproved(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        record(event, AuditSubjectType.INVESTOR, uuid(payload, "investorId"), null,
                "KYC approved for investor " + text(payload, "investorId"));
    }

    @Transactional
    public void onKycRevoked(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        record(event, AuditSubjectType.INVESTOR, uuid(payload, "investorId"), null,
                "KYC revoked: " + text(payload, "reason"));
    }

    @Transactional
    public void onTradeSettled(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        record(event, AuditSubjectType.TRADE, uuid(payload, "tradeId"), null,
                "Trade settled orderId=" + text(payload, "orderId"));
    }

    @Transactional
    public void onDocumentUploaded(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        record(event, AuditSubjectType.DOCUMENT, uuid(payload, "documentId"), null,
                "Document uploaded type=" + text(payload, "documentType"));
    }

    @Transactional
    public void onOrderMatched(KafkaJsonEvent event) {
        JsonNode payload = event.payload();
        record(event, AuditSubjectType.ORDER, uuid(payload, "orderId"), null,
                "Order matched flatId=" + text(payload, "flatId"));
    }

    @Transactional(readOnly = true)
    public Page<AuditEntry> findByInvestor(UUID investorId, Pageable pageable) {
        return repository.findBySubjectTypeAndSubjectIdOrderByOccurredAtDesc(
                AuditSubjectType.INVESTOR, investorId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEntry> findByFlat(UUID flatId, Pageable pageable) {
        return repository.findBySubjectTypeAndSubjectIdOrderByOccurredAtDesc(
                AuditSubjectType.FLAT, flatId, pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditEntry> exportAll() {
        return repository.findAll();
    }

    private static UUID uuid(JsonNode node, String field) {
        return UUID.fromString(node.path(field).asText());
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
