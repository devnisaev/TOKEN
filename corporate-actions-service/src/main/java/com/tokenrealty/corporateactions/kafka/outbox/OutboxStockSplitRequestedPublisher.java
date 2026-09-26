package com.tokenrealty.corporateactions.kafka.outbox;

import com.tokenrealty.corporateactions.kafka.CorporateActionsKafkaEventTypes;
import com.tokenrealty.corporateactions.kafka.events.StockSplitRequestedEvent;
import com.tokenrealty.corporateactions.kafka.port.StockSplitRequestedPublisher;
import com.tokenrealty.outbox.OutboxPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxStockSplitRequestedPublisher implements StockSplitRequestedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.stock-split-requested:"
            + CorporateActionsKafkaEventTypes.STOCK_SPLIT_REQUESTED + "}")
    private String stockSplitRequestedTopic;

    @Override
    public void publish(StockSplitRequestedEvent event) {
        OutboxPayload.start()
                .put("corporateActionId", event.corporateActionId())
                .put("flatId", event.flatId())
                .putIfPresent("contractId", event.contractId())
                .put("period", event.period())
                .put("splitRatio", event.splitRatio())
                .enqueue(outboxWriter, stockSplitRequestedTopic, event.corporateActionId());
    }
}
