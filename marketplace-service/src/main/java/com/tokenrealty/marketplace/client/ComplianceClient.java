package com.tokenrealty.marketplace.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.web.exception.ComplianceBlockedException;
import com.tokenrealty.web.exception.ValidationException;
import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class ComplianceClient extends DownstreamRestClientSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_COUNTRY = "US";

    public ComplianceClient(@Qualifier("complianceRestClient") RestClient restClient) {
        super(restClient);
    }

    public boolean isWalletApproved(String walletAddress) {
        ComplianceCheckResponse response = checkWallet(walletAddress);
        return response != null && response.whitelisted();
    }

    public ComplianceCheckResponse checkWallet(String walletAddress) {
        return getAllowNull(
                "/v1/compliance/check/{walletAddress}",
                ComplianceCheckResponse.class,
                DownstreamServices.COMPLIANCE,
                walletAddress);
    }

    public void checkInvestment(UUID investorId, String countryCode, BigDecimal amountUsd) {
        String jurisdiction = countryCode != null && !countryCode.isBlank() ? countryCode : DEFAULT_COUNTRY;
        InvestmentCheckRequest request = new InvestmentCheckRequest(investorId, jurisdiction, amountUsd);
        try {
            InvestmentCheckResponse response = http().post(
                    "/v1/compliance/check-investment",
                    request,
                    InvestmentCheckResponse.class);
            if (response != null && !response.allowed()) {
                throw new ComplianceBlockedException("Investment not allowed for jurisdiction " + jurisdiction);
            }
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 422) {
                throw new ComplianceBlockedException(problemDetail(ex));
            }
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ValidationException(DownstreamServices.COMPLIANCE.label() + " unavailable");
            }
            throw new ValidationException(DownstreamServices.COMPLIANCE.label() + " rejected request: "
                    + problemDetail(ex));
        } catch (ResourceAccessException ex) {
            throw new ValidationException(DownstreamServices.COMPLIANCE.label() + " unavailable");
        }
    }

    public record ComplianceCheckResponse(
            String walletAddress,
            @JsonProperty("isWhitelisted") boolean whitelisted,
            String status,
            UUID investorId,
            String countryCode,
            Instant expiresAt
    ) {
    }

    public record InvestmentCheckRequest(
            UUID investorId,
            String countryCode,
            BigDecimal amountUsd
    ) {
    }

    private static String problemDetail(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            try {
                JsonNode node = MAPPER.readTree(body);
                if (node.hasNonNull("detail")) {
                    return node.get("detail").asText();
                }
                if (node.hasNonNull("title")) {
                    return node.get("title").asText();
                }
            } catch (Exception ignored) {
                return body.length() > 200 ? body.substring(0, 200) : body;
            }
        }
        return ex.getStatusText();
    }

    public record InvestmentCheckResponse(
            UUID investorId,
            String jurisdiction,
            BigDecimal amountUsd,
            @JsonProperty("isAllowed") boolean allowed,
            BigDecimal minInvestmentUsd
    ) {
    }
}
