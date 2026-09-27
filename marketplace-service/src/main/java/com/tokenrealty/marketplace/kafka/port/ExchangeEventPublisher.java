package com.tokenrealty.marketplace.kafka.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface ExchangeEventPublisher {

    void publishOrderPlaced(OrderPlacedEvent event);

    void publishOrderCancelled(OrderCancelledEvent event);

    void publishTradeExecuted(TradeExecutedEvent event);

    record OrderPlacedEvent(
            UUID orderId,
            UUID contractId,
            UUID flatId,
            String side,
            BigDecimal limitPriceUsd,
            long quantity,
            UUID investorId
    ) {
    }

    record OrderCancelledEvent(
            UUID orderId,
            UUID contractId,
            UUID investorId,
            long cancelledRemaining
    ) {
    }

    record TradeExecutedEvent(
            UUID fillId,
            UUID contractId,
            UUID flatId,
            UUID bidOrderId,
            UUID askOrderId,
            UUID buyerId,
            UUID sellerId,
            BigDecimal pricePerTokenUsd,
            long tokenAmount,
            BigDecimal totalPriceUsd
    ) {
    }
}
