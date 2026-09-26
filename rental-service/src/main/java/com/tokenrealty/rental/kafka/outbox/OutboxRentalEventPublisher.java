package com.tokenrealty.rental.kafka.outbox;

import com.tokenrealty.outbox.OutboxPayload;
import com.tokenrealty.rental.kafka.RentalKafkaEventTypes;
import com.tokenrealty.rental.kafka.port.RentalEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRentalEventPublisher implements RentalEventPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.rent-due:" + RentalKafkaEventTypes.RENT_DUE + "}")
    private String rentDueTopic;

    @Value("${tokenrealty.kafka.topic.lease-expired:" + RentalKafkaEventTypes.LEASE_EXPIRED + "}")
    private String leaseExpiredTopic;

    @Override
    public void publishRentDue(RentDueEvent event) {
        OutboxPayload.start()
                .put("leaseId", event.leaseId())
                .put("flatId", event.flatId())
                .put("tenantId", event.tenantId())
                .put("amountUsd", event.amountUsd().toPlainString())
                .put("period", event.period())
                .put("dueDate", event.dueDate().toString())
                .enqueue(outboxWriter, rentDueTopic, event.leaseId().toString());
    }

    @Override
    public void publishLeaseExpired(LeaseExpiredEvent event) {
        OutboxPayload.start()
                .put("leaseId", event.leaseId())
                .put("flatId", event.flatId())
                .put("tenantId", event.tenantId())
                .put("endDate", event.endDate().toString())
                .put("expiredAt", event.expiredAt().toString())
                .enqueue(outboxWriter, leaseExpiredTopic, event.leaseId().toString());
    }
}
