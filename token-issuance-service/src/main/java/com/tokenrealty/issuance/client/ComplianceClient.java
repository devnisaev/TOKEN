package com.tokenrealty.issuance.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ComplianceClient {

    private final RestClient restClient;

    public ComplianceClient(@Qualifier("complianceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean isWalletApproved(String walletAddress) {
        ComplianceCheckResponse response = checkWallet(walletAddress);
        return response != null && response.whitelisted();
    }

    public ComplianceCheckResponse checkWallet(String walletAddress) {
        return restClient.get()
                .uri("/v1/compliance/check/{walletAddress}", walletAddress)
                .retrieve()
                .body(ComplianceCheckResponse.class);
    }

    public record ComplianceCheckResponse(
            String walletAddress,
            @JsonProperty("isWhitelisted") boolean whitelisted,
            String status,
            java.util.UUID investorId
    ) {
    }
}
