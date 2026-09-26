package com.tokenrealty.wallet.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class IssuanceClient extends DownstreamRestClientSupport {

    public IssuanceClient(@Qualifier("issuanceRestClient") RestClient restClient) {
        super(restClient);
    }

    public List<TokenHolderView> getHoldings(UUID investorId) {
        List<TokenHolderView> holdings = getAllowNotFound(
                uriBuilder -> uriBuilder.path("/v1/investors/{investorId}/holdings").build(investorId),
                new ParameterizedTypeReference<>() {
                },
                DownstreamServices.TOKEN_ISSUANCE);
        return holdings != null ? holdings : List.of();
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
