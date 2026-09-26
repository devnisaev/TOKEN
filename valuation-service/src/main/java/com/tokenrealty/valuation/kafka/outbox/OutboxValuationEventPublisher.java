package com.tokenrealty.valuation.kafka.outbox;

import com.tokenrealty.outbox.OutboxPayload;
import com.tokenrealty.valuation.kafka.ValuationKafkaEventTypes;
import com.tokenrealty.valuation.kafka.port.ValuationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxValuationEventPublisher implements ValuationEventPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.valuation-updated:" + ValuationKafkaEventTypes.VALUATION_UPDATED + "}")
    private String valuationUpdatedTopic;

    @Override
    public void publishValuationUpdated(ValuationUpdatedEvent event) {
        OutboxPayload.start()
                .put("flatId", event.flatId())
                .put("buildingId", event.buildingId())
                .put("valuationRequestId", event.valuationRequestId())
                .put("valueUsd", event.valueUsd().toPlainString())
                .put("totalTokens", event.totalTokens())
                .put("navPerTokenUsd", event.navPerTokenUsd().toPlainString())
                .put("approvedAt", event.approvedAt().toString())
                .enqueue(outboxWriter, valuationUpdatedTopic, event.flatId().toString());
    }
}
