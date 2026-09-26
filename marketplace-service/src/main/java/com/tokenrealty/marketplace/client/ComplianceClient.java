package com.tokenrealty.marketplace.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tokenrealty.web.exception.ValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ComplianceClient {

    private final RestClient restClient;

    public ComplianceClient(@Qualifier("complianceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean isWalletApproved(String walletAddress) {
        try {
            ComplianceCheckResponse response = restClient.get()
                    .uri("/v1/compliance/check/{walletAddress}", walletAddress)
                    .retrieve()
                    .body(ComplianceCheckResponse.class);
            return response != null && response.whitelisted();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ValidationException("Compliance service unavailable");
            }
            throw new ValidationException("Compliance check failed: " + ex.getStatusText());
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Compliance service unavailable");
        }
    }

    public record ComplianceCheckResponse(
            String walletAddress,
            @JsonProperty("isWhitelisted") boolean whitelisted,
            String status
    ) {
    }
}
