package com.tokenrealty.payment.kafka.outbox;

import com.tokenrealty.outbox.OutboxPayload;
import com.tokenrealty.payment.kafka.PaymentKafkaEventTypes;
import com.tokenrealty.payment.kafka.events.RentCollectedEvent;
import com.tokenrealty.payment.kafka.port.RentCollectedPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxRentCollectedPublisher implements RentCollectedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.rent-collected:" + PaymentKafkaEventTypes.RENT_COLLECTED + "}")
    private String rentCollectedTopic;

    @Override
    public void publishRentCollected(RentCollectedEvent event) {
        OutboxPayload.start()
                .put("payoutId", event.payoutId())
                .putIfPresent("leaseId", event.leaseId())
                .putIfPresent("flatId", event.flatId())
                .put("tenantId", event.tenantId())
                .put("period", event.period())
                .put("amount", Map.of(
                        "value", event.amount().value(),
                        "currency", event.amount().currency().name()))
                .put("txHash", event.txHash())
                .enqueue(outboxWriter, rentCollectedTopic, event.payoutId());
    }
}
