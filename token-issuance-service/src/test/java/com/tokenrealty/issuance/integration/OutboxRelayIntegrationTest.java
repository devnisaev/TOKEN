package com.tokenrealty.issuance.integration;

import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.outbox.OutboxEvent;
import com.tokenrealty.issuance.kafka.outbox.OutboxEventRepository;
import com.tokenrealty.issuance.kafka.outbox.OutboxRelayWorker;
import com.tokenrealty.kafka.testsupport.OutboxKafkaListenerTestConfiguration;
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
        "tokenrealty.kafka.test.consumer-group=issuance-outbox-it"
})
@DisplayName("Issuance outbox relay integration test")
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
        UUID contractId = UUID.randomUUID();
        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("issuance")
                .aggregateId(contractId)
                .eventType(IssuanceKafkaEventTypes.TRANSFER_COMPLETED)
                .payload("{\"eventId\":\"" + UUID.randomUUID() + "\"}")
                .status(OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .retryCount(0)
                .build());

        when(kafkaTemplate.send(
                eq(IssuanceKafkaEventTypes.TRANSFER_COMPLETED),
                eq(contractId.toString()),
                anyString()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        outboxRelayWorker.relayPending();

        assertThat(outboxEventRepository.findAll())
                .singleElement()
                .extracting(OutboxEvent::getStatus)
                .isEqualTo(OutboxStatus.PUBLISHED);
    }
}
