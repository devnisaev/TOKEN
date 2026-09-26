package com.tokenrealty.corporateactions.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.corporateactions.client.TokenIssuanceClient;
import com.tokenrealty.corporateactions.entity.CorporateActionStatus;
import com.tokenrealty.corporateactions.entity.CorporateActionType;
import com.tokenrealty.corporateactions.kafka.CorporateActionsKafkaEventTypes;
import com.tokenrealty.corporateactions.kafka.command.DividendDistributedCommand;
import com.tokenrealty.corporateactions.kafka.command.RentCollectedCommand;
import com.tokenrealty.corporateactions.repository.CorporateActionRepository;
import com.tokenrealty.corporateactions.service.CorporateActionsService;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class CorporateActionsKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired CorporateActionsService corporateActionsService;
    @Autowired CorporateActionRepository repository;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean TokenIssuanceClient tokenIssuanceClient;

    @BeforeEach
    void clean() {
        repository.deleteAll();
        when(tokenIssuanceClient.findContractIdByFlatId(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("rent.collected creates REQUESTED dividend corporate action")
    void rentCollected_createsAction() throws Exception {
        UUID flatId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(tokenIssuanceClient.findContractIdByFlatId(flatId)).thenReturn(Optional.of(contractId));

        publishRentCollected(UUID.randomUUID(), flatId, "2025-09", "1500.00");

        assertThat(repository.count()).isEqualTo(1);
        var action = repository.findAll().getFirst();
        assertThat(action.getType()).isEqualTo(CorporateActionType.DIVIDEND);
        assertThat(action.getStatus()).isEqualTo(CorporateActionStatus.REQUESTED);
        assertThat(action.getFlatId()).isEqualTo(flatId);
        assertThat(action.getContractId()).isEqualTo(contractId);
        assertThat(action.getPeriod()).isEqualTo("2025-09");
        assertThat(action.getGrossAmountUsd()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("duplicate rent.collected eventId is deduped")
    void duplicateEventId_deduped() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();
        publishRentCollected(eventId, flatId, "2025-09", "1500.00");
        publishRentCollected(eventId, flatId, "2025-09", "1500.00");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("dividend.distributed marks matching action COMPLETED")
    void dividendDistributed_marksCompleted() throws Exception {
        UUID flatId = UUID.randomUUID();
        publishRentCollected(UUID.randomUUID(), flatId, "2025-10", "900.00");

        publishDividendDistributed(flatId, "2025-10");

        var action = repository.findAll().getFirst();
        assertThat(action.getStatus()).isEqualTo(CorporateActionStatus.COMPLETED);
    }

    private void publishRentCollected(UUID eventId, UUID flatId, String period, String amount)
            throws Exception {
        Map<String, Object> payload = Map.of(
                "leaseId", UUID.randomUUID().toString(),
                "flatId", flatId.toString(),
                "tenantId", UUID.randomUUID().toString(),
                "period", period,
                "amount", Map.of("value", amount, "currency", "USDC"),
                "txHash", "0xRentCollected");
        publish(CorporateActionsKafkaEventTypes.RENT_COLLECTED, eventId, payload,
                event -> corporateActionsService.onRentCollected(RentCollectedCommand.from(event)));
    }

    private void publishDividendDistributed(UUID flatId, String period) throws Exception {
        Map<String, Object> payload = Map.of(
                "contractId", UUID.randomUUID().toString(),
                "flatId", flatId.toString(),
                "period", period,
                "totalAmount", Map.of("value", "900.00", "currency", "USDC"));
        publish(CorporateActionsKafkaEventTypes.DIVIDEND_DISTRIBUTED, UUID.randomUUID(), payload,
                event -> corporateActionsService.onDividendDistributed(DividendDistributedCommand.from(event)));
    }

    private void publish(String eventType, UUID eventId, Map<String, Object> payload,
                         java.util.function.Consumer<com.tokenrealty.events.kafka.KafkaJsonEvent> handler)
            throws Exception {
        EventEnvelope<Map<String, Object>> envelope = new EventEnvelope<>(
                eventId, eventType, Instant.parse("2025-09-25T16:00:00Z"), null, payload);
        eventConsumer.consume(objectMapper.writeValueAsString(envelope), eventType, "test", handler);
    }
}
