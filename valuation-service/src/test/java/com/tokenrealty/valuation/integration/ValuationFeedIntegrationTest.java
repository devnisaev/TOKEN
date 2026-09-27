package com.tokenrealty.valuation.integration;

import com.tokenrealty.valuation.repository.ValuationRequestRepository;
import com.tokenrealty.valuation.service.ValuationFeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ValuationFeedIntegrationTest {

    @Autowired ValuationFeedService valuationFeedService;
    @Autowired ValuationRequestRepository valuationRequestRepository;

    @BeforeEach
    void clean() {
        valuationRequestRepository.deleteAll();
    }

    @Test
    @DisplayName("AVM feed ingest creates pending valuation request")
    void ingestFeed_createsPendingRequest() {
        UUID buildingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();
        String body = """
                {"buildingId":"%s","flatId":"%s","valueUsd":"850000.00","totalTokens":1000}
                """.formatted(buildingId, flatId);

        var response = valuationFeedService.ingestFeed("corelogic", body);

        assertThat(response.status().name()).isEqualTo("PENDING");
        assertThat(response.flatId()).isEqualTo(flatId);
        assertThat(valuationRequestRepository.count()).isEqualTo(1);
    }
}
