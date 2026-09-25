package com.tokenrealty.payment.kafka.in;

import com.tokenrealty.payment.kafka.PaymentKafkaEventTypes;
import com.tokenrealty.payment.kafka.command.OrderMatchedCommand;
import com.tokenrealty.payment.service.OrderEscrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class OrderMatchedListener {

    private final PaymentKafkaIngestSupport ingestSupport;
    private final OrderEscrowService orderEscrowService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.order-matched}")
    public void onOrderMatched(String message) {
        ingestSupport.consume(message, PaymentKafkaEventTypes.ORDER_MATCHED,
                "Order matched reconciliation failed",
                event -> orderEscrowService.ensureEscrowLinked(OrderMatchedCommand.from(event)));
    }
}
