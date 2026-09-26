package com.tokenrealty.marketplace.integration;

import com.tokenrealty.kafka.testsupport.OutboxKafkaListenerTestConfiguration;
import com.tokenrealty.marketplace.kafka.MarketplaceKafkaEventTypes;
import com.tokenrealty.marketplace.kafka.outbox.OutboxEvent;
import com.tokenrealty.marketplace.kafka.outbox.OutboxEventRepository;
import com.tokenrealty.marketplace.kafka.outbox.OutboxRelayWorker;
import com.tokenrealty.outbox.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Import(OutboxKafkaListenerTestConfiguration.class)
@TestPropertySource(properties = {
        "tokenrealty.kafka.enabled=true",
        "tokenrealty.kafka.test.consumer-group=marketplace-outbox-it"
})
@DisplayName("Marketplace outbox relay integration test")
class OutboxRelayIntegrationTest {

    @Autowired OutboxRelayWorker outboxRelayWorker;
    @Autowired OutboxEventRepository outboxEventRepository;

    @MockitoBean KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void clearOutbox() {
        outboxEventRepository.deleteAll();
    }

    @Test
    @DisplayName("relayPending marks outbox row PUBLISHED after Kafka ack")
    void relayPending_marksPublished() {
        UUID orderId = UUID.randomUUID();
        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("marketplace")
                .aggregateId(orderId)
                .eventType(MarketplaceKafkaEventTypes.ORDER_MATCHED)
                .payload("{\"eventId\":\"" + UUID.randomUUID() + "\"}")
                .status(OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .retryCount(0)
                .build());

        when(kafkaTemplate.send(
                eq(MarketplaceKafkaEventTypes.ORDER_MATCHED),
                eq(orderId.toString()),
                anyString()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        outboxRelayWorker.relayPending();

        OutboxEvent published = outboxEventRepository.findAll().getFirst();
        assertThat(published.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isNotNull();
    }
}
