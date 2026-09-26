package com.tokenrealty.payment.kafka.outbox;

import com.tokenrealty.outbox.OutboxPayload;
import com.tokenrealty.payment.kafka.PaymentKafkaEventTypes;
import com.tokenrealty.payment.kafka.events.PayoutCompletedEvent;
import com.tokenrealty.payment.kafka.port.PayoutCompletedPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPayoutCompletedPublisher implements PayoutCompletedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.payout-completed:" + PaymentKafkaEventTypes.PAYOUT_COMPLETED + "}")
    private String payoutCompletedTopic;

    @Override
    public void publishPayoutCompleted(PayoutCompletedEvent event) {
        OutboxPayload.start()
                .put("payoutId", event.payoutId())
                .putIfPresent("dividendPaymentId", event.dividendPaymentId())
                .put("recipientInvestorId", event.recipientInvestorId())
                .put("recipientWallet", event.recipientWallet())
                .put("purpose", event.purpose().name())
                .put("txHash", event.txHash())
                .put("completedAt", event.completedAt().toString())
                .enqueue(outboxWriter, payoutCompletedTopic, event.payoutId());
    }
}
