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

    private final NotificationKafkaIngestSupport ingestSupport;
    private final NotificationLogService notificationLogService;

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

    private void ingest(String message, String eventType) {
        ingestSupport.consume(message, eventType, "Notification processing failed",
                event -> notificationLogService.logEvent(eventType, event));
    }
}
