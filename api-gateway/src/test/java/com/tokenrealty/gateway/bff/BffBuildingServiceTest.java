package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.PropertyRegistryClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BffBuildingService unit tests")
class BffBuildingServiceTest {

    @Mock PropertyRegistryClient registryClient;
    @InjectMocks BffBuildingService buildingService;

    @Test
    @DisplayName("getBuildingDetail counts tokenized and available flats")
    void getBuildingDetail_countsFlats() {
        UUID buildingId = UUID.randomUUID();
        when(registryClient.getBuilding(buildingId)).thenReturn(new PropertyRegistryClient.BuildingDetailView(
                buildingId,
                "Sunrise Tower",
                "1 Main St",
                "Bishkek",
                "KG",
                "720000",
                10,
                20,
                2020,
                5000.0,
                "ACTIVE",
                "RESIDENTIAL",
                "CAD-1",
                "A+",
                "C-2",
                2022,
                2,
                List.of(
                        new PropertyRegistryClient.FlatSummaryView(
                                UUID.randomUUID(), "101", 1, 65.0, "TOKENIZED", BigDecimal.TEN),
                        new PropertyRegistryClient.FlatSummaryView(
                                UUID.randomUUID(), "102", 1, 70.0, "AVAILABLE", null)),
                null));

        var detail = buildingService.getBuildingDetail(buildingId);

        assertThat(detail.building().name()).isEqualTo("Sunrise Tower");
        assertThat(detail.tokenizedFlatCount()).isEqualTo(1);
        assertThat(detail.availableFlatCount()).isEqualTo(1);
    }
}
