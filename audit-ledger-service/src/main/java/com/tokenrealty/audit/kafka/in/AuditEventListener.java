package com.tokenrealty.audit.kafka.in;

import com.tokenrealty.audit.kafka.AuditKafkaEventTypes;
import com.tokenrealty.audit.service.AuditLedgerService;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AuditEventListener {

    private final KafkaEventConsumer eventConsumer;
    private final AuditLedgerService auditLedgerService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.kyc-approved}")
    public void onKycApproved(String message) {
        ingest(message, AuditKafkaEventTypes.KYC_APPROVED, auditLedgerService::onKycApproved);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.kyc-revoked}")
    public void onKycRevoked(String message) {
        ingest(message, AuditKafkaEventTypes.KYC_REVOKED, auditLedgerService::onKycRevoked);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.trade-settled}")
    public void onTradeSettled(String message) {
        ingest(message, AuditKafkaEventTypes.TRADE_SETTLED, auditLedgerService::onTradeSettled);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.document-uploaded}")
    public void onDocumentUploaded(String message) {
        ingest(message, AuditKafkaEventTypes.DOCUMENT_UPLOADED, auditLedgerService::onDocumentUploaded);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.order-matched}")
    public void onOrderMatched(String message) {
        ingest(message, AuditKafkaEventTypes.ORDER_MATCHED, auditLedgerService::onOrderMatched);
    }

    private void ingest(String message, String eventType,
                        java.util.function.Consumer<com.tokenrealty.events.kafka.KafkaJsonEvent> handler) {
        eventConsumer.consume(message, eventType, "Audit ledger ingest failed", handler);
    }
}
