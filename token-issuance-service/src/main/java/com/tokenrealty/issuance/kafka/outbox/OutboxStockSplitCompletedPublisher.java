package com.tokenrealty.issuance.kafka.outbox;

import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.port.StockSplitCompletedPublisher;
import com.tokenrealty.outbox.OutboxPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxStockSplitCompletedPublisher implements StockSplitCompletedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.stock-split-completed:" + IssuanceKafkaEventTypes.STOCK_SPLIT_COMPLETED + "}")
    private String stockSplitCompletedTopic;

    @Override
    public void publishStockSplitCompleted(StockSplitCompletedEvent event) {
        OutboxPayload.start()
                .put("corporateActionId", event.corporateActionId())
                .put("contractId", event.contractId())
                .put("flatId", event.flatId())
                .put("period", event.period())
                .put("splitRatio", event.splitRatio().toPlainString())
                .put("newTotalSupply", event.newTotalSupply())
                .put("newTokenPriceUsd", event.newTokenPriceUsd().toPlainString())
                .put("completedAt", event.completedAt().toString())
                .enqueue(outboxWriter, stockSplitCompletedTopic, event.contractId());
    }
}
