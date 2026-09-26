package com.tokenrealty.indexer.kafka.outbox;

import com.tokenrealty.indexer.kafka.IndexerKafkaEventTypes;
import com.tokenrealty.outbox.OutboxPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxIndexedEventPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.transfer-indexed:" + IndexerKafkaEventTypes.TRANSFER_INDEXED + "}")
    private String transferIndexedTopic;

    public void publishIndexedEvent(
            UUID indexedEventId,
            String eventType,
            String contractAddress,
            String txHash,
            int logIndex,
            long blockNumber
    ) {
        OutboxPayload.start()
                .put("indexedEventId", indexedEventId)
                .put("eventType", eventType)
                .put("contractAddress", contractAddress)
                .put("txHash", txHash)
                .put("logIndex", logIndex)
                .put("blockNumber", blockNumber)
                .enqueue(outboxWriter, transferIndexedTopic, indexedEventId);
    }
}
