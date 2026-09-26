package com.tokenrealty.gateway.client;

import com.tokenrealty.web.exception.ValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TokenIssuanceClient {

    private final RestClient restClient;

    public TokenIssuanceClient(@Qualifier("tokenIssuanceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public TokenContractView getContractByFlatId(UUID flatId) {
        try {
            return restClient.get()
                    .uri("/v1/tokens/by-flat/{flatId}", flatId)
                    .retrieve()
                    .body(TokenContractView.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            throw unavailable(ex);
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Token Issuance service unavailable");
        }
    }

    private static ValidationException unavailable(RestClientResponseException ex) {
        if (ex.getStatusCode().is5xxServerError()) {
            return new ValidationException("Token Issuance service unavailable");
        }
        return new ValidationException("Token Issuance request failed: " + ex.getStatusText());
    }

    public record TokenContractView(
            UUID id,
            UUID flatId,
            String contractAddress,
            String tokenSymbol,
            Long totalSupply,
            BigDecimal tokenPriceUsd,
            String status
    ) {
    }
}
