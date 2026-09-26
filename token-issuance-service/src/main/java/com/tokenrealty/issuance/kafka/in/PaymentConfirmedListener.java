package com.tokenrealty.issuance.kafka.in;

import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.command.PaymentConfirmedCommand;
import com.tokenrealty.issuance.service.PaymentTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class PaymentConfirmedListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final PaymentTransferService paymentTransferService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.payment-confirmed}")
    public void onPaymentConfirmed(String message) {
        eventConsumer.consume(message, IssuanceKafkaEventTypes.PAYMENT_CONFIRMED,
                "Payment confirmed transfer failed",
                event -> paymentTransferService.executeTransfer(PaymentConfirmedCommand.from(event)));
    }
}
