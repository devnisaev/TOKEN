package com.tokenrealty.compliance.kafka.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.outbox.OutboxStatus;
import com.tokenrealty.outbox.relay.OutboxRelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayWorker {

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${tokenrealty.kafka.relay.publish-timeout-ms:10000}")
    private long publishTimeoutMs;

    @Value("${tokenrealty.kafka.relay.max-retries:5}")
    private int maxRetries;

    @Value("${tokenrealty.kafka.serialization:json}")
    private String serializationFormat;

    @Scheduled(fixedDelayString = "${tokenrealty.kafka.relay.poll-ms:1000}")
    public void relayPending() {
        var pending = repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        OutboxRelay.relay(
                pending,
                kafkaTemplate,
                publishTimeoutMs,
                maxRetries,
                repository::save,
                log,
                OutboxRelay.resolvePayloadTransform(serializationFormat, objectMapper));
    }
}
