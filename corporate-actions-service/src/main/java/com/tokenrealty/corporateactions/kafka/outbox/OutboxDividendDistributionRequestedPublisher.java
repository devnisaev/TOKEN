package com.tokenrealty.corporateactions.kafka.outbox;

import com.tokenrealty.corporateactions.kafka.CorporateActionsKafkaEventTypes;
import com.tokenrealty.corporateactions.kafka.events.DividendDistributionRequestedEvent;
import com.tokenrealty.corporateactions.kafka.port.DividendDistributionRequestedPublisher;
import com.tokenrealty.outbox.OutboxPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxDividendDistributionRequestedPublisher implements DividendDistributionRequestedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.dividend-distribution-requested:"
            + CorporateActionsKafkaEventTypes.DIVIDEND_DISTRIBUTION_REQUESTED + "}")
    private String dividendDistributionRequestedTopic;

    @Override
    public void publish(DividendDistributionRequestedEvent event) {
        OutboxPayload.start()
                .put("corporateActionId", event.corporateActionId())
                .put("flatId", event.flatId())
                .putIfPresent("contractId", event.contractId())
                .put("period", event.period())
                .put("grossAmountUsd", event.grossAmountUsd())
                .enqueue(outboxWriter, dividendDistributionRequestedTopic, event.corporateActionId());
    }
}
