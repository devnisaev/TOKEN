package com.tokenrealty.marketplace.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TokenIssuanceClient {

    private final RestClient restClient;

    public TokenIssuanceClient(@Qualifier("tokenIssuanceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ComplianceCheckResponse checkWallet(String walletAddress) {
        return restClient.get()
                .uri("/v1/compliance/check/{walletAddress}", walletAddress)
                .retrieve()
                .body(ComplianceCheckResponse.class);
    }

    public record ComplianceCheckResponse(
            String walletAddress,
            boolean whitelisted,
            String status
    ) {
    }
}
