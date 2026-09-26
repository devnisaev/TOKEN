package com.tokenrealty.valuation.integration;

import com.tokenrealty.valuation.dto.ValuationDtos.NavSnapshotResponse;
import com.tokenrealty.valuation.dto.ValuationDtos.SubmitValuationRequest;
import com.tokenrealty.valuation.dto.ValuationDtos.ValuationRequestResponse;
import com.tokenrealty.valuation.entity.ValuationRequestStatus;
import com.tokenrealty.valuation.kafka.ValuationKafkaEventTypes;
import com.tokenrealty.valuation.kafka.outbox.OutboxEventRepository;
import com.tokenrealty.valuation.repository.NavSnapshotRepository;
import com.tokenrealty.valuation.service.ValuationService;
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

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "tokenrealty.kafka.enabled=true")
@DisplayName("Valuation workflow integration test")
class ValuationWorkflowTest {

    @Autowired ValuationService valuationService;
    @Autowired NavSnapshotRepository navSnapshotRepository;
    @Autowired OutboxEventRepository outboxEventRepository;

    @MockitoBean KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void cleanOutbox() {
        outboxEventRepository.deleteAll();
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));
    }

    @Test
    void submitThenApprove_createsNavSnapshot() {
        UUID appraiserId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID buildingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();

        ValuationRequestResponse submitted = valuationService.submit(
                new SubmitValuationRequest(buildingId, flatId, new BigDecimal("1000000.00"), 1000L, "Annual appraisal"),
                appraiserId);

        assertThat(submitted.status()).isEqualTo(ValuationRequestStatus.PENDING);
        assertThat(submitted.submittedBy()).isEqualTo(appraiserId);

        ValuationRequestResponse approved = valuationService.approve(submitted.id(), adminId);

        assertThat(approved.status()).isEqualTo(ValuationRequestStatus.APPROVED);
        assertThat(approved.reviewedBy()).isEqualTo(adminId);
        assertThat(approved.navSnapshotId()).isNotNull();

        NavSnapshotResponse nav = valuationService.getLatestNav(flatId);

        assertThat(nav.flatId()).isEqualTo(flatId);
        assertThat(nav.buildingId()).isEqualTo(buildingId);
        assertThat(nav.valueUsd()).isEqualByComparingTo("1000000.00");
        assertThat(nav.totalTokens()).isEqualTo(1000L);
        assertThat(nav.navPerTokenUsd()).isEqualByComparingTo("1000.00000000");
        assertThat(navSnapshotRepository.count()).isEqualTo(1);

        assertThat(outboxEventRepository.findAll())
                .extracting(com.tokenrealty.valuation.kafka.outbox.OutboxEvent::getEventType)
                .containsExactlyInAnyOrder(
                        ValuationKafkaEventTypes.VALUATION_UPDATED,
                        ValuationKafkaEventTypes.VALUATION_APPROVED);
    }
}
