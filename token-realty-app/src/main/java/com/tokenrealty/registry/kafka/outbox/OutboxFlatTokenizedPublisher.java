package com.tokenrealty.registry.kafka.outbox;

import com.tokenrealty.outbox.OutboxPayload;
import com.tokenrealty.registry.kafka.RegistryKafkaEventTypes;
import com.tokenrealty.registry.kafka.port.FlatTokenizedPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxFlatTokenizedPublisher implements FlatTokenizedPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.flat-tokenized:" + RegistryKafkaEventTypes.FLAT_TOKENIZED + "}")
    private String flatTokenizedTopic;

    @Override
    public void publishFlatTokenized(FlatTokenizedEvent event) {
        OutboxPayload.start()
                .put("flatId", event.flatId())
                .put("buildingId", event.buildingId())
                .put("contractAddress", event.contractAddress())
                .put("totalTokens", event.totalTokens())
                .put("tokenPriceUsd", event.tokenPriceUsd())
                .enqueue(outboxWriter, flatTokenizedTopic, event.flatId());
    }
}
