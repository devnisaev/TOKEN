package com.tokenrealty.payment.kafka.in;

import com.tokenrealty.payment.kafka.PaymentKafkaEventTypes;
import com.tokenrealty.payment.kafka.command.DividendDistributedCommand;
import com.tokenrealty.payment.service.DividendPayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DividendDistributedListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final DividendPayoutService dividendPayoutService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.dividend-distributed}")
    public void onDividendDistributed(String message) {
        eventConsumer.consume(message, PaymentKafkaEventTypes.DIVIDEND_DISTRIBUTED,
                "Dividend distributed processing failed",
                event -> dividendPayoutService.createHolderPayouts(DividendDistributedCommand.from(event)));
    }
}
