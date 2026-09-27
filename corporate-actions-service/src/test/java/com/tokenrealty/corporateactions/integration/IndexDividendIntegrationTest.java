package com.tokenrealty.corporateactions.integration;

import com.tokenrealty.corporateactions.dto.CorporateActionDtos.*;
import com.tokenrealty.corporateactions.kafka.command.PayoutCompletedCommand;
import com.tokenrealty.corporateactions.repository.IndexDividendAccrualRepository;
import com.tokenrealty.corporateactions.service.IndexBasketService;
import com.tokenrealty.corporateactions.service.IndexDividendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Index dividend aggregation — Phase 16 integration")
class IndexDividendIntegrationTest {

    @Autowired IndexBasketService indexBasketService;
    @Autowired IndexDividendService indexDividendService;
    @Autowired IndexDividendAccrualRepository indexDividendAccrualRepository;

    UUID indexId;
    UUID contractId;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        IndexDefinitionResponse index = indexBasketService.create(CreateIndexDefinitionRequest.builder()
                .name("Yield Basket")
                .symbol("YLD")
                .constituents(List.of(
                        new IndexConstituentRequest(contractId, 7000),
                        new IndexConstituentRequest(other, 3000)))
                .build());
        indexId = index.id();
        indexBasketService.activate(indexId);
    }

    @Test
    @DisplayName("Payout on constituent accrues weighted index share")
    void accruesIndexDividend() {
        UUID payoutId = UUID.randomUUID();
        indexDividendService.onPayoutCompleted(new PayoutCompletedCommand(
                UUID.randomUUID(), payoutId, contractId, new BigDecimal("1000.00")));

        assertThat(indexDividendAccrualRepository.findByIndexIdOrderByCreatedAtDesc(indexId))
                .hasSize(1)
                .first()
                .satisfies(accrual -> assertThat(accrual.getIndexShareUsd())
                        .isEqualByComparingTo("700.00"));
    }
}
