package com.tokenrealty.marketplace.kafka.outbox;

import com.tokenrealty.marketplace.kafka.MarketplaceKafkaEventTypes;
import com.tokenrealty.marketplace.kafka.port.ExchangeEventPublisher;
import com.tokenrealty.outbox.OutboxPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxExchangeEventPublisher implements ExchangeEventPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.exchange-order-placed:" + MarketplaceKafkaEventTypes.EXCHANGE_ORDER_PLACED + "}")
    private String orderPlacedTopic;

    @Value("${tokenrealty.kafka.topic.exchange-order-cancelled:" + MarketplaceKafkaEventTypes.EXCHANGE_ORDER_CANCELLED + "}")
    private String orderCancelledTopic;

    @Value("${tokenrealty.kafka.topic.exchange-trade-executed:" + MarketplaceKafkaEventTypes.EXCHANGE_TRADE_EXECUTED + "}")
    private String tradeExecutedTopic;

    @Override
    public void publishOrderPlaced(OrderPlacedEvent event) {
        OutboxPayload.start()
                .put("orderId", event.orderId())
                .put("contractId", event.contractId())
                .put("flatId", event.flatId())
                .put("side", event.side())
                .put("limitPriceUsd", event.limitPriceUsd())
                .put("quantity", event.quantity())
                .put("investorId", event.investorId())
                .enqueue(outboxWriter, orderPlacedTopic, event.orderId());
    }

    @Override
    public void publishOrderCancelled(OrderCancelledEvent event) {
        OutboxPayload.start()
                .put("orderId", event.orderId())
                .put("contractId", event.contractId())
                .put("investorId", event.investorId())
                .put("cancelledRemaining", event.cancelledRemaining())
                .enqueue(outboxWriter, orderCancelledTopic, event.orderId());
    }

    @Override
    public void publishTradeExecuted(TradeExecutedEvent event) {
        OutboxPayload.start()
                .put("fillId", event.fillId())
                .put("contractId", event.contractId())
                .put("flatId", event.flatId())
                .put("bidOrderId", event.bidOrderId())
                .put("askOrderId", event.askOrderId())
                .put("buyerId", event.buyerId())
                .put("sellerId", event.sellerId())
                .put("pricePerTokenUsd", event.pricePerTokenUsd())
                .put("tokenAmount", event.tokenAmount())
                .put("totalPriceUsd", event.totalPriceUsd())
                .enqueue(outboxWriter, tradeExecutedTopic, event.fillId());
    }
}
