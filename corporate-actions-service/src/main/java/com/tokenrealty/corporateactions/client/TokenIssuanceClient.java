package com.tokenrealty.corporateactions.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
public class TokenIssuanceClient extends DownstreamRestClientSupport {

    public TokenIssuanceClient(@Qualifier("tokenIssuanceRestClient") RestClient restClient) {
        super(restClient);
    }

    public Optional<UUID> findContractIdByFlatId(UUID flatId) {
        TokenContractResponse response = getAllowNotFound(
                "/v1/tokens/by-flat/{flatId}",
                TokenContractResponse.class,
                DownstreamServices.TOKEN_ISSUANCE,
                flatId);
        return response != null ? Optional.of(response.id()) : Optional.empty();
    }

    public record TokenContractResponse(
            UUID id,
            UUID flatId,
            String spvWalletAddress,
            String contractAddress,
            Long totalSupply,
            BigDecimal tokenPriceUsd,
            String status
    ) {
    }
}
