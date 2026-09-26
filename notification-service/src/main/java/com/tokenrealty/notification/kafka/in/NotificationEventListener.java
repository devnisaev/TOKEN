package com.tokenrealty.notification.kafka.in;

import com.tokenrealty.notification.kafka.NotificationKafkaEventTypes;
import com.tokenrealty.notification.service.NotificationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class NotificationEventListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final NotificationLogService notificationLogService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.building-approved}")
    public void onBuildingApproved(String message) {
        ingest(message, NotificationKafkaEventTypes.BUILDING_APPROVED);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.flat-tokenized}")
    public void onFlatTokenized(String message) {
        ingest(message, NotificationKafkaEventTypes.FLAT_TOKENIZED);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.listing-created}")
    public void onListingCreated(String message) {
        ingest(message, NotificationKafkaEventTypes.LISTING_CREATED);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.order-matched}")
    public void onOrderMatched(String message) {
        ingest(message, NotificationKafkaEventTypes.ORDER_MATCHED);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.payment-confirmed}")
    public void onPaymentConfirmed(String message) {
        ingest(message, NotificationKafkaEventTypes.PAYMENT_CONFIRMED);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.transfer-completed}")
    public void onTransferCompleted(String message) {
        ingest(message, NotificationKafkaEventTypes.TRANSFER_COMPLETED);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.kyc-approved}")
    public void onKycApproved(String message) {
        ingest(message, NotificationKafkaEventTypes.KYC_APPROVED);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.kyc-revoked}")
    public void onKycRevoked(String message) {
        ingest(message, NotificationKafkaEventTypes.KYC_REVOKED);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.trade-settled}")
    public void onTradeSettled(String message) {
        ingest(message, NotificationKafkaEventTypes.TRADE_SETTLED);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.dividend-distributed}")
    public void onDividendDistributed(String message) {
        ingest(message, NotificationKafkaEventTypes.DIVIDEND_DISTRIBUTED);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.rent-collected}")
    public void onRentCollected(String message) {
        ingest(message, NotificationKafkaEventTypes.RENT_COLLECTED);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.rent-due}")
    public void onRentDue(String message) {
        ingest(message, NotificationKafkaEventTypes.RENT_DUE);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.lease-expired}")
    public void onLeaseExpired(String message) {
        ingest(message, NotificationKafkaEventTypes.LEASE_EXPIRED);
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.document-uploaded}")
    public void onDocumentUploaded(String message) {
        ingest(message, NotificationKafkaEventTypes.DOCUMENT_UPLOADED);
    }

    private void ingest(String message, String eventType) {
        eventConsumer.consume(message, eventType, "Notification processing failed",
                event -> notificationLogService.logEvent(eventType, event));
    }
}
