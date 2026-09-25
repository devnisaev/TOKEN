package com.tokenrealty.marketplace.kafka.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface OrderMatchedPublisher {

    void publishOrderMatched(OrderMatchedEvent event);

    record OrderMatchedEvent(
            UUID orderId,
            UUID tradeId,
            UUID listingId,
            UUID flatId,
            UUID contractId,
            UUID buyerId,
            UUID sellerId,
            long tokenAmount,
            BigDecimal totalPriceUsd,
            UUID paymentId
    ) {
    }
}
