package com.tokenrealty.marketplace.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class TokenIssuanceClient extends DownstreamRestClientSupport {

    public TokenIssuanceClient(@Qualifier("tokenIssuanceRestClient") RestClient restClient) {
        super(restClient);
    }

    public long getHolderBalance(UUID contractId, String walletAddress) {
        HolderBalanceResponse response = getAllowNotFound(
                "/v1/tokens/{contractId}/holders/by-wallet/{walletAddress}",
                HolderBalanceResponse.class,
                DownstreamServices.TOKEN_ISSUANCE,
                contractId,
                walletAddress);
        return response != null ? response.balance() : 0L;
    }

    public TokenContractResponse getContractByFlatId(UUID flatId) {
        return get(
                "/v1/tokens/by-flat/{flatId}",
                TokenContractResponse.class,
                DownstreamServices.TOKEN_ISSUANCE,
                flatId);
    }

    public TokenTransferResponse transferTokens(UUID contractId, TransferRequest request) {
        return post(
                "/v1/tokens/{contractId}/transfers",
                request,
                TokenTransferResponse.class,
                DownstreamServices.TOKEN_ISSUANCE,
                contractId);
    }

    public record HolderBalanceResponse(
            UUID contractId,
            String walletAddress,
            long balance
    ) {
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

    public record TransferRequest(
            String fromAddress,
            String toAddress,
            long amount,
            BigDecimal pricePerTokenUsd
    ) {
    }

    public record TokenTransferResponse(
            UUID id,
            UUID contractId,
            String txHash,
            String status
    ) {
    }
}
