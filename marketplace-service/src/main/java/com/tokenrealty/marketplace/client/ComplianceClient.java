package com.tokenrealty.marketplace.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ComplianceClient extends DownstreamRestClientSupport {

    public ComplianceClient(@Qualifier("complianceRestClient") RestClient restClient) {
        super(restClient);
    }

    public boolean isWalletApproved(String walletAddress) {
        ComplianceCheckResponse response = getAllowNull(
                "/v1/compliance/check/{walletAddress}",
                ComplianceCheckResponse.class,
                DownstreamServices.COMPLIANCE,
                walletAddress);
        return response != null && response.whitelisted();
    }

    public record ComplianceCheckResponse(
            String walletAddress,
            @JsonProperty("isWhitelisted") boolean whitelisted,
            String status
    ) {
    }
}
