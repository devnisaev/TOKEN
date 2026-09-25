package com.tokenrealty.notification.kafka;

public final class NotificationKafkaEventTypes {

    public static final String FLAT_TOKENIZED = "tokenrealty.registry.flat.tokenized.v1";
    public static final String LISTING_CREATED = "tokenrealty.marketplace.listing.created.v1";
    public static final String ORDER_MATCHED = "tokenrealty.marketplace.order.matched.v1";
    public static final String PAYMENT_CONFIRMED = "tokenrealty.payment.payment.confirmed.v1";
    public static final String TRANSFER_COMPLETED = "tokenrealty.issuance.transfer.completed.v1";

    private NotificationKafkaEventTypes() {
    }
}
