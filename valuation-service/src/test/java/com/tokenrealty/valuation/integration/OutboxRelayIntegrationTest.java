package com.tokenrealty.valuation.integration;

import com.tokenrealty.outbox.OutboxStatus;
import com.tokenrealty.valuation.kafka.ValuationKafkaEventTypes;
import com.tokenrealty.valuation.kafka.outbox.OutboxEvent;
import com.tokenrealty.valuation.kafka.outbox.OutboxEventRepository;
import com.tokenrealty.valuation.kafka.outbox.OutboxRelayWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
@TestPropertySource(properties = "tokenrealty.kafka.enabled=true")
@DisplayName("Valuation outbox relay integration test")
class OutboxRelayIntegrationTest {

    @Autowired OutboxRelayWorker outboxRelayWorker;
    @Autowired OutboxEventRepository outboxEventRepository;

    @MockitoBean KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void clearOutbox() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void relayPending_marksPublished() {
        UUID flatId = UUID.randomUUID();
        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("valuation")
                .aggregateId(flatId)
                .eventType(ValuationKafkaEventTypes.VALUATION_UPDATED)
                .payload("{\"eventId\":\"" + UUID.randomUUID() + "\"}")
                .status(OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .retryCount(0)
                .build());

        when(kafkaTemplate.send(
                eq(ValuationKafkaEventTypes.VALUATION_UPDATED),
                eq(flatId.toString()),
                anyString()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        outboxRelayWorker.relayPending();

        assertThat(outboxEventRepository.findAll())
                .singleElement()
                .extracting(OutboxEvent::getStatus)
                .isEqualTo(OutboxStatus.PUBLISHED);
    }
}
