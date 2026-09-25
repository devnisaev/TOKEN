package com.tokenrealty.marketplace.kafka;

public final class MarketplaceKafkaEventTypes {

    private MarketplaceKafkaEventTypes() {
    }

    public static final String LISTING_CREATED = "tokenrealty.marketplace.listing.created.v1";
    public static final String ORDER_MATCHED = "tokenrealty.marketplace.order.matched.v1";
    public static final String TRADE_SETTLED = "tokenrealty.marketplace.trade.settled.v1";
}
