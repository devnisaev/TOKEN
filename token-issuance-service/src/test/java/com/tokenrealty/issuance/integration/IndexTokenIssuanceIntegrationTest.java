package com.tokenrealty.issuance.integration;

import com.tokenrealty.issuance.dto.IssuanceDtos.IssueIndexTokenRequest;
import com.tokenrealty.issuance.dto.IssuanceDtos.TokenContractResponse;
import com.tokenrealty.issuance.service.IndexTokenIssuanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Index token issuance — Phase 16 integration")
class IndexTokenIssuanceIntegrationTest {

    @Autowired IndexTokenIssuanceService indexTokenIssuanceService;

    @Test
    @DisplayName("Register index wrapper token")
    void issueIndexToken() {
        UUID indexId = UUID.randomUUID();
        TokenContractResponse response = indexTokenIssuanceService.issueIndexToken(
                new IssueIndexTokenRequest(
                        indexId,
                        "US Commercial Operators",
                        "USCO",
                        1_000_000L,
                        new BigDecimal("100.00"),
                        "0xspv"));
        assertThat(response.tokenSymbol()).isEqualTo("USCO");
        assertThat(response.flatId()).isEqualTo(indexId);
        assertThat(response.tokenName()).startsWith("INDEX:");
    }
}
