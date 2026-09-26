package com.tokenrealty.indexer.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class IssuanceClient {

    private final RestClient restClient;

    public IssuanceClient(@Qualifier("issuanceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<TokenContractView> listContracts() {
        try {
            var page = restClient.get()
                    .uri("/v1/tokens?size=100")
                    .retrieve()
                    .body(new ParameterizedTypeReference<SpringPage<TokenContractView>>() {
                    });
            return page != null ? page.content() : List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    public List<TokenHolderView> listHolders(UUID contractId) {
        try {
            var page = restClient.get()
                    .uri("/v1/tokens/{contractId}/holders?size=200", contractId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<SpringPage<TokenHolderView>>() {
                    });
            return page != null ? page.content() : List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    public record SpringPage<T>(List<T> content) {
    }

    public record TokenContractView(
            UUID id,
            UUID flatId,
            String contractAddress,
            String tokenSymbol,
            Long totalSupply
    ) {
    }

    public record TokenHolderView(
            UUID id,
            UUID contractId,
            UUID investorId,
            String walletAddress,
            long balance
    ) {
    }
}
