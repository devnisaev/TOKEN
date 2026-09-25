package com.tokenrealty.payment.kafka.outbox;

import com.tokenrealty.outbox.OutboxPayload;
import com.tokenrealty.payment.kafka.PaymentKafkaEventTypes;
import com.tokenrealty.payment.kafka.events.PaymentConfirmedEvent;
import com.tokenrealty.payment.kafka.port.PaymentConfirmedPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxPaymentConfirmedPublisher implements PaymentConfirmedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.payment-confirmed:" + PaymentKafkaEventTypes.PAYMENT_CONFIRMED + "}")
    private String paymentConfirmedTopic;

    @Override
    public void publishPaymentConfirmed(PaymentConfirmedEvent event) {
        OutboxPayload.start()
                .put("paymentId", event.paymentId())
                .put("orderId", event.orderId())
                .put("payerId", event.payerId())
                .put("amount", Map.of(
                        "value", event.amount().value(),
                        "currency", event.amount().currency().name()))
                .put("txHash", event.txHash())
                .put("confirmedAt", event.confirmedAt().toString())
                .enqueue(outboxWriter, paymentConfirmedTopic, event.paymentId());
    }
}
