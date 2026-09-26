package com.tokenrealty.marketplace.client;

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

    public long getHolderBalance(UUID contractId, String walletAddress) {
        try {
            HolderBalanceResponse response = restClient.get()
                    .uri("/v1/tokens/{contractId}/holders/by-wallet/{walletAddress}",
                            contractId, walletAddress)
                    .retrieve()
                    .body(HolderBalanceResponse.class);
            return response != null ? response.balance() : 0L;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return 0L;
            }
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ValidationException("Token Issuance service unavailable");
            }
            throw new ValidationException("Holder lookup failed: " + ex.getStatusText());
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Token Issuance service unavailable");
        }
    }

    public TokenContractResponse getContractByFlatId(UUID flatId) {
        try {
            return restClient.get()
                    .uri("/v1/tokens/by-flat/{flatId}", flatId)
                    .retrieve()
                    .body(TokenContractResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ValidationException("Token Issuance service unavailable");
            }
            throw new ValidationException("Contract lookup failed: " + ex.getStatusText());
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Token Issuance service unavailable");
        }
    }

    public TokenTransferResponse transferTokens(UUID contractId, TransferRequest request) {
        try {
            return restClient.post()
                    .uri("/v1/tokens/{contractId}/transfers", contractId)
                    .body(request)
                    .retrieve()
                    .body(TokenTransferResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ValidationException("Token Issuance service unavailable");
            }
            throw new ValidationException("Token transfer failed: " + ex.getStatusText());
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Token Issuance service unavailable");
        }
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
