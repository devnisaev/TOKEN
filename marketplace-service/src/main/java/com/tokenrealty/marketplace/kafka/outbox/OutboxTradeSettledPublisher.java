package com.tokenrealty.marketplace.kafka.outbox;

import com.tokenrealty.outbox.OutboxPayload;
import com.tokenrealty.marketplace.kafka.MarketplaceKafkaEventTypes;
import com.tokenrealty.marketplace.kafka.port.TradeSettledPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxTradeSettledPublisher implements TradeSettledPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.trade-settled:" + MarketplaceKafkaEventTypes.TRADE_SETTLED + "}")
    private String tradeSettledTopic;

    @Override
    public void publishTradeSettled(TradeSettledEvent event) {
        OutboxPayload.start()
                .put("tradeId", event.tradeId())
                .put("orderId", event.orderId())
                .put("listingId", event.listingId())
                .putIfPresent("paymentId", event.paymentId())
                .putIfPresent("transferId", event.transferId())
                .enqueue(outboxWriter, tradeSettledTopic, event.tradeId());
    }
}
