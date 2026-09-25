package com.tokenrealty.issuance.kafka.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayWorker {

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${tokenrealty.kafka.relay.publish-timeout-ms:10000}")
    private long publishTimeoutMs;

    @Value("${tokenrealty.kafka.relay.max-retries:5}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${tokenrealty.kafka.relay.poll-ms:1000}")
    public void relayPending() {
        List<OutboxEvent> pending = repository.findTop50ByStatusOrderByCreatedAtAsc(
                OutboxEvent.OutboxStatus.PENDING);
        for (OutboxEvent event : pending) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            kafkaTemplate.send(event.getEventType(), event.getAggregateId().toString(), event.getPayload())
                    .get(publishTimeoutMs, TimeUnit.MILLISECONDS);
            event.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
        } catch (Exception ex) {
            event.setRetryCount(event.getRetryCount() + 1);
            if (event.getRetryCount() >= maxRetries) {
                event.setStatus(OutboxEvent.OutboxStatus.FAILED);
            }
            log.warn("Outbox relay failed for {} (attempt {}): {}",
                    event.getId(), event.getRetryCount(), ex.getMessage());
        }
        repository.save(event);
    }
}
