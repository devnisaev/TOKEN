package com.tokenrealty.issuance.kafka.in;

import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.command.PayoutCompletedCommand;
import com.tokenrealty.issuance.service.DividendService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class PayoutCompletedListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final DividendService dividendService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.payout-completed}")
    public void onPayoutCompleted(String message) {
        eventConsumer.consume(message, IssuanceKafkaEventTypes.PAYOUT_COMPLETED,
                "Payout completed processing failed",
                event -> {
                    PayoutCompletedCommand command = PayoutCompletedCommand.from(event);
                    dividendService.markPaid(
                            command.dividendPaymentId(), command.txHash(), command.completedAt());
                });
    }
}
