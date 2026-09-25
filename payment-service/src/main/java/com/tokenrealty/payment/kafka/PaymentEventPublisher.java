package com.tokenrealty.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.payment.entity.Payment;
import com.tokenrealty.payment.entity.Payout;
import com.tokenrealty.payment.kafka.events.PaymentConfirmedEvent;
import com.tokenrealty.payment.kafka.events.RentCollectedEvent;
import com.tokenrealty.payment.kafka.outbox.OutboxEvent;
import com.tokenrealty.payment.kafka.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${tokenrealty.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public void publishPaymentConfirmed(Payment payment) {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getPayerId(),
                new PaymentConfirmedEvent.Amount(
                        payment.getAmount().toPlainString(), payment.getCurrency()),
                payment.getTxHash(),
                payment.getConfirmedAt()
        );
        enqueue(PaymentKafkaEventTypes.PAYMENT_CONFIRMED, payment.getId(), event);
    }

    public void publishRentCollected(Payout payout) {
        RentCollectedEvent event = new RentCollectedEvent(
                payout.getId(),
                payout.getReferenceId(),
                null,
                payout.getRecipientInvestorId(),
                payout.getPeriod(),
                new RentCollectedEvent.Amount(
                        payout.getAmount().toPlainString(), payout.getCurrency()),
                payout.getTxHash()
        );
        enqueue(PaymentKafkaEventTypes.RENT_COLLECTED, payout.getId(), event);
    }

    private void enqueue(String eventType, UUID aggregateId, Object payload) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled — skipping outbox enqueue for {}", eventType);
            return;
        }
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("payment")
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .traceId(UUID.randomUUID().toString())
                    .build();
            outboxRepository.save(event);
        } catch (Exception ex) {
            log.warn("Failed to enqueue outbox event {}: {}", eventType, ex.getMessage());
        }
    }
}
