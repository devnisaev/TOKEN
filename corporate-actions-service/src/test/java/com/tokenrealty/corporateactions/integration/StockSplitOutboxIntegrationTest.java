package com.tokenrealty.corporateactions.integration;

import com.tokenrealty.corporateactions.client.TokenIssuanceClient;
import com.tokenrealty.corporateactions.dto.CorporateActionDtos.RequestStockSplitRequest;
import com.tokenrealty.corporateactions.kafka.CorporateActionsKafkaEventTypes;
import com.tokenrealty.corporateactions.kafka.outbox.OutboxEventRepository;
import com.tokenrealty.corporateactions.service.CorporateActionsService;
import com.tokenrealty.outbox.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "tokenrealty.kafka.enabled=true")
class StockSplitOutboxIntegrationTest {

    @Autowired CorporateActionsService corporateActionsService;
    @Autowired OutboxEventRepository outboxEventRepository;

    @MockitoBean TokenIssuanceClient tokenIssuanceClient;
    @MockitoBean KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void clean() {
        outboxEventRepository.deleteAll();
        when(tokenIssuanceClient.findContractIdByFlatId(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("request stock split enqueues stock-split.requested outbox row")
    void requestStockSplit_enqueuesOutbox() {
        UUID flatId = UUID.randomUUID();

        var view = corporateActionsService.requestStockSplit(new RequestStockSplitRequest(
                flatId,
                new BigDecimal("2.0000"),
                "2025-Q3"));

        assertThat(outboxEventRepository.count()).isEqualTo(1);
        var outbox = outboxEventRepository.findAll().getFirst();
        assertThat(outbox.getEventType()).isEqualTo(CorporateActionsKafkaEventTypes.STOCK_SPLIT_REQUESTED);
        assertThat(outbox.getAggregateId()).isEqualTo(view.id());
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }
}
