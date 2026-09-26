package com.tokenrealty.gateway.graphql;

import com.tokenrealty.gateway.bff.*;
import com.tokenrealty.gateway.dto.BffDtos.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BFF GraphQL controller")
class BffGraphQlControllerTest {

    @Mock BffFlatService flatService;
    @Mock BffListingService listingService;
    @Mock BffBuildingService buildingService;
    @Mock BffPortfolioService portfolioService;
    @Mock BffTenantLeaseService tenantLeaseService;
    @Mock BffTenantMaintenanceService tenantMaintenanceService;
    @Mock BffAdminMaintenanceService adminMaintenanceService;

    @InjectMocks BffGraphQlController controller;

    @Test
    void flatDetailDelegatesToBffService() {
        UUID flatId = UUID.randomUUID();
        FlatDetailResponse response = FlatDetailResponse.builder()
                .flatId(flatId)
                .buildingId(UUID.randomUUID())
                .buildingName("Tower A")
                .flatNumber("12A")
                .status("TOKENIZED")
                .build();
        when(flatService.getFlatDetail(flatId)).thenReturn(response);

        assertThat(controller.flatDetail(flatId.toString()).buildingName()).isEqualTo("Tower A");
    }

    @Test
    void buildingDetailDelegatesToBffService() {
        UUID buildingId = UUID.randomUUID();
        when(buildingService.getBuildingDetail(buildingId)).thenReturn(BuildingBffDetailResponse.builder()
                .tokenizedFlatCount(2)
                .availableFlatCount(1)
                .build());

        assertThat(controller.buildingDetail(buildingId.toString()).tokenizedFlatCount()).isEqualTo(2);
    }

    @Test
    void adminMaintenanceQueueDelegatesToBffService() {
        when(adminMaintenanceService.getMaintenanceQueue()).thenReturn(List.of());

        assertThat(controller.adminMaintenanceQueue()).isEmpty();
    }

    @Test
    void investorPortfolioDelegatesToBffService() {
        UUID investorId = UUID.randomUUID();
        when(portfolioService.getPortfolio(investorId)).thenReturn(PortfolioBffResponse.builder()
                .balance(PortfolioBalanceView.builder()
                        .investorId(investorId)
                        .primaryWalletAddress("0xabc")
                        .fiatBalances(List.of())
                        .tokenHoldings(List.of())
                        .build())
                .recentDividends(List.of())
                .build());

        assertThat(controller.investorPortfolio(investorId.toString()).balance().investorId())
                .isEqualTo(investorId);
    }
}
