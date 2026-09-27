package com.tokenrealty.marketplace.kafka;

public final class MarketplaceKafkaEventTypes {

    private MarketplaceKafkaEventTypes() {
    }

    public static final String LISTING_CREATED = "tokenrealty.marketplace.listing.created.v1";
    public static final String ORDER_MATCHED = "tokenrealty.marketplace.order.matched.v1";
    public static final String TRADE_SETTLED = "tokenrealty.marketplace.trade.settled.v1";
    public static final String EXCHANGE_ORDER_PLACED = "tokenrealty.marketplace.order.placed.v1";
    public static final String EXCHANGE_ORDER_CANCELLED = "tokenrealty.marketplace.order.cancelled.v1";
    public static final String EXCHANGE_TRADE_EXECUTED = "tokenrealty.marketplace.trade.executed.v1";

    public static final String FLAT_TOKENIZED = "tokenrealty.registry.flat.tokenized.v1";
    public static final String BUILDING_APPROVED = "tokenrealty.registry.building.approved.v1";
    public static final String PAYMENT_CONFIRMED = "tokenrealty.payment.payment.confirmed.v1";
    public static final String TRANSFER_COMPLETED = "tokenrealty.issuance.transfer.completed.v1";
}
