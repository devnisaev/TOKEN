package com.tokenrealty.payment.kafka;

public final class PaymentKafkaEventTypes {

    public static final String PAYMENT_CONFIRMED = "tokenrealty.payment.payment.confirmed.v1";
    public static final String RENT_COLLECTED = "tokenrealty.payment.rent.collected.v1";
    public static final String ORDER_MATCHED = "tokenrealty.marketplace.order.matched.v1";
    public static final String DIVIDEND_DISTRIBUTED = "tokenrealty.issuance.dividend.distributed.v1";

    private PaymentKafkaEventTypes() {
    }
}
