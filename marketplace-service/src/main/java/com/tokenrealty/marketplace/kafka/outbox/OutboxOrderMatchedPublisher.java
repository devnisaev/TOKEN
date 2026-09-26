package com.tokenrealty.marketplace.kafka.outbox;

import com.tokenrealty.outbox.OutboxPayload;
import com.tokenrealty.marketplace.kafka.MarketplaceKafkaEventTypes;
import com.tokenrealty.marketplace.kafka.port.OrderMatchedPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxOrderMatchedPublisher implements OrderMatchedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.order-matched:" + MarketplaceKafkaEventTypes.ORDER_MATCHED + "}")
    private String orderMatchedTopic;

    @Override
    public void publishOrderMatched(OrderMatchedEvent event) {
        OutboxPayload.start()
                .put("orderId", event.orderId())
                .put("tradeId", event.tradeId())
                .put("listingId", event.listingId())
                .put("flatId", event.flatId())
                .putIfPresent("contractId", event.contractId())
                .put("buyerId", event.buyerId())
                .putIfPresent("sellerId", event.sellerId())
                .put("tokenAmount", event.tokenAmount())
                .put("totalPriceUsd", event.totalPriceUsd())
                .putIfPresent("paymentId", event.paymentId())
                .enqueue(outboxWriter, orderMatchedTopic, event.orderId());
    }
}
