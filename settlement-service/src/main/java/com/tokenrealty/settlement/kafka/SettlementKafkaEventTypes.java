package com.tokenrealty.settlement.kafka;

public final class SettlementKafkaEventTypes {

    public static final String ORDER_MATCHED = "tokenrealty.marketplace.order.matched.v1";
    public static final String PAYMENT_CONFIRMED = "tokenrealty.payment.payment.confirmed.v1";
    public static final String TRANSFER_COMPLETED = "tokenrealty.issuance.transfer.completed.v1";
    public static final String TRADE_SETTLED = "tokenrealty.marketplace.trade.settled.v1";
    public static final String SETTLEMENT_STUCK = "tokenrealty.settlement.stuck.v1";
    public static final String SETTLEMENT_RECOVERED = "tokenrealty.settlement.recovered.v1";

    private SettlementKafkaEventTypes() {
    }
}
