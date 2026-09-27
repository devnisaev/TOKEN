package com.tokenrealty.integration.integration;

import com.tokenrealty.integration.client.PropertyRegistryClient;
import com.tokenrealty.integration.dto.IntegrationDtos.IngestIotReadingRequest;
import com.tokenrealty.integration.service.IotFeedService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("IoT feed ingest — Phase 17 integration")
class IotFeedIntegrationTest {

    @Autowired IotFeedService iotFeedService;

    @MockitoBean PropertyRegistryClient propertyRegistryClient;

    @Test
    @DisplayName("Ingest reading forwards occupancy to Property Registry")
    void ingestReading() {
        UUID flatId = UUID.randomUUID();
        var response = iotFeedService.ingest(IngestIotReadingRequest.builder()
                .flatId(flatId)
                .provider("sensor-co")
                .occupancyPct(new BigDecimal("85.0"))
                .build());

        assertThat(response.occupancyPct()).isEqualByComparingTo("85.0");
        verify(propertyRegistryClient).updateFlatOccupancy(flatId, new BigDecimal("85.0"));
    }
}
