package com.tokenrealty.outbox.relay;

import org.slf4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class OutboxRelay {

    private OutboxRelay() {
    }

    public static <E extends OutboxRelayTarget> void relay(
            List<E> pending,
            KafkaTemplate<String, String> kafkaTemplate,
            long publishTimeoutMs,
            int maxRetries,
            Consumer<E> saver,
            Logger log
    ) {
        relay(pending, kafkaTemplate, publishTimeoutMs, maxRetries, saver, log, null);
    }

    public static <E extends OutboxRelayTarget> void relay(
            List<E> pending,
            KafkaTemplate<String, String> kafkaTemplate,
            long publishTimeoutMs,
            int maxRetries,
            Consumer<E> saver,
            Logger log,
            UnaryOperator<String> payloadTransform
    ) {
        for (E event : pending) {
            publishOne(event, kafkaTemplate, publishTimeoutMs, maxRetries, saver, log, payloadTransform);
        }
    }

    private static <E extends OutboxRelayTarget> void publishOne(
            E event,
            KafkaTemplate<String, String> kafkaTemplate,
            long publishTimeoutMs,
            int maxRetries,
            Consumer<E> saver,
            Logger log,
            UnaryOperator<String> payloadTransform
    ) {
        try {
            String body = payloadTransform != null ? payloadTransform.apply(event.getPayload()) : event.getPayload();
            kafkaTemplate.send(event.getEventType(), event.getAggregateId().toString(), body)
                    .get(publishTimeoutMs, TimeUnit.MILLISECONDS);
            event.markPublished(Instant.now());
            log.debug("Published outbox event {} to {}", event.getId(), event.getEventType());
        } catch (Exception ex) {
            event.setRetryCount(event.getRetryCount() + 1);
            if (event.getRetryCount() >= maxRetries) {
                event.markFailed();
            }
            log.warn("Outbox relay failed for {} (attempt {}): {}",
                    event.getId(), event.getRetryCount(), ex.getMessage());
        }
        saver.accept(event);
    }
}
