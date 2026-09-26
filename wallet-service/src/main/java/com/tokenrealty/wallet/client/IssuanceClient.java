package com.tokenrealty.wallet.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class IssuanceClient {

    private final RestClient restClient;

    public IssuanceClient(@Qualifier("issuanceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<TokenHolderView> getHoldings(UUID investorId) {
        try {
            return restClient.get()
                    .uri("/v1/investors/{investorId}/holdings", investorId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (Exception ex) {
            return List.of();
        }
    }

    public record TokenHolderView(
            UUID id,
            UUID contractId,
            UUID investorId,
            String walletAddress,
            long balance,
            BigDecimal balanceUsd,
            BigDecimal ownershipPercentage,
            String status,
            Instant createdAt,
            String tokenSymbol
    ) {
    }
}
