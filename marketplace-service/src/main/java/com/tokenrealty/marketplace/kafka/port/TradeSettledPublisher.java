package com.tokenrealty.marketplace.kafka.port;

import java.util.UUID;

public interface TradeSettledPublisher {

    void publishTradeSettled(TradeSettledEvent event);

    record TradeSettledEvent(
            UUID tradeId,
            UUID orderId,
            UUID listingId,
            UUID paymentId,
            UUID transferId
    ) {
    }
}
