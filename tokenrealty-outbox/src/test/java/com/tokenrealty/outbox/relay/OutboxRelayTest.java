package com.tokenrealty.outbox.relay;

import com.tokenrealty.outbox.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("OutboxRelay unit tests")
class OutboxRelayTest {

    @Test
    void relay_marksPublishedOnKafkaAck() {
        MutableOutboxEvent event = new MutableOutboxEvent();
        event.setEventType("topic.test");
        event.setAggregateId(UUID.randomUUID());
        event.setPayload("{\"eventId\":\"abc\"}");

        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(eq(event.getEventType()), eq(event.getAggregateId().toString()), anyString()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        List<MutableOutboxEvent> saved = new ArrayList<>();
        OutboxRelay.relay(
                List.of(event),
                kafkaTemplate,
                5_000,
                3,
                saved::add,
                LoggerFactory.getLogger(OutboxRelayTest.class));

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(saved).containsExactly(event);
    }

    private static final class MutableOutboxEvent implements OutboxRelayTarget {
        private UUID id = UUID.randomUUID();
        private String eventType;
        private UUID aggregateId;
        private String payload;
        private OutboxStatus status = OutboxStatus.PENDING;
        private int retryCount;
        private Instant publishedAt;

        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public String getEventType() {
            return eventType;
        }

        void setEventType(String eventType) {
            this.eventType = eventType;
        }

        @Override
        public UUID getAggregateId() {
            return aggregateId;
        }

        void setAggregateId(UUID aggregateId) {
            this.aggregateId = aggregateId;
        }

        @Override
        public String getPayload() {
            return payload;
        }

        void setPayload(String payload) {
            this.payload = payload;
        }

        OutboxStatus getStatus() {
            return status;
        }

        Instant getPublishedAt() {
            return publishedAt;
        }

        @Override
        public int getRetryCount() {
            return retryCount;
        }

        @Override
        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }

        @Override
        public void markPublished(Instant publishedAt) {
            this.status = OutboxStatus.PUBLISHED;
            this.publishedAt = publishedAt;
        }

        @Override
        public void markFailed() {
            this.status = OutboxStatus.FAILED;
        }
    }
}
