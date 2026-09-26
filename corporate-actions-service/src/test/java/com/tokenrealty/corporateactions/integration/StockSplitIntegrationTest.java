package com.tokenrealty.corporateactions.integration;

import com.tokenrealty.corporateactions.client.TokenIssuanceClient;
import com.tokenrealty.corporateactions.dto.CorporateActionDtos.RequestStockSplitRequest;
import com.tokenrealty.corporateactions.entity.CorporateActionStatus;
import com.tokenrealty.corporateactions.entity.CorporateActionType;
import com.tokenrealty.corporateactions.repository.CorporateActionRepository;
import com.tokenrealty.corporateactions.service.CorporateActionsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class StockSplitIntegrationTest {

    @Autowired CorporateActionsService corporateActionsService;
    @Autowired CorporateActionRepository repository;

    @MockitoBean TokenIssuanceClient tokenIssuanceClient;

    @BeforeEach
    void clean() {
        repository.deleteAll();
        when(tokenIssuanceClient.findContractIdByFlatId(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("request stock split creates REQUESTED corporate action")
    void requestStockSplit_createsAction() {
        UUID flatId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(tokenIssuanceClient.findContractIdByFlatId(flatId)).thenReturn(Optional.of(contractId));

        var view = corporateActionsService.requestStockSplit(new RequestStockSplitRequest(
                flatId,
                new BigDecimal("2.0000"),
                "2025-Q3"));

        assertThat(view.type()).isEqualTo(CorporateActionType.STOCK_SPLIT);
        assertThat(view.status()).isEqualTo(CorporateActionStatus.REQUESTED);
        assertThat(view.flatId()).isEqualTo(flatId);
        assertThat(view.contractId()).isEqualTo(contractId);
        assertThat(view.splitRatio()).isEqualByComparingTo(new BigDecimal("2.0000"));
        assertThat(repository.count()).isEqualTo(1);
    }
}
