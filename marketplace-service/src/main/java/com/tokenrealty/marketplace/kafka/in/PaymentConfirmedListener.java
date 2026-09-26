package com.tokenrealty.marketplace.kafka.in;

import com.tokenrealty.marketplace.kafka.MarketplaceKafkaEventTypes;
import com.tokenrealty.marketplace.kafka.command.PaymentConfirmedCommand;
import com.tokenrealty.marketplace.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class PaymentConfirmedListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final OrderService orderService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.payment-confirmed}")
    public void onPaymentConfirmed(String message) {
        eventConsumer.consume(message, MarketplaceKafkaEventTypes.PAYMENT_CONFIRMED,
                "Payment confirmed processing failed",
                event -> orderService.onPaymentConfirmed(PaymentConfirmedCommand.from(event)));
    }
}
