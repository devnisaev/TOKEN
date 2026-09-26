package com.tokenrealty.issuance.kafka;

public final class IssuanceKafkaEventTypes {

    public static final String PAYMENT_CONFIRMED = "tokenrealty.payment.payment.confirmed.v1";
    public static final String RENT_COLLECTED = "tokenrealty.payment.rent.collected.v1";
    public static final String TRANSFER_COMPLETED = "tokenrealty.issuance.transfer.completed.v1";
    public static final String DIVIDEND_DISTRIBUTED = "tokenrealty.issuance.dividend.distributed.v1";

    private IssuanceKafkaEventTypes() {
    }
}
