package com.tokenrealty.reporting.kafka;

public final class ReportingKafkaEventTypes {

    public static final String TRADE_SETTLED = "tokenrealty.marketplace.trade.settled.v1";
    public static final String ORDER_MATCHED = "tokenrealty.marketplace.order.matched.v1";
    public static final String DIVIDEND_DISTRIBUTED = "tokenrealty.issuance.dividend.distributed.v1";
    public static final String RENT_COLLECTED = "tokenrealty.payment.rent.collected.v1";
    public static final String FLAT_TOKENIZED = "tokenrealty.registry.flat.tokenized.v1";
    public static final String SETTLEMENT_STUCK = "tokenrealty.settlement.stuck.v1";
    public static final String VALUATION_APPROVED = "tokenrealty.valuation.approved.v1";

    private ReportingKafkaEventTypes() {
    }
}
