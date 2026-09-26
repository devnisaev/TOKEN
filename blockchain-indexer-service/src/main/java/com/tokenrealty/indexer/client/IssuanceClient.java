package com.tokenrealty.indexer.client;

import com.tokenrealty.web.exception.ValidationException;
import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class IssuanceClient extends DownstreamRestClientSupport {

    public IssuanceClient(@Qualifier("issuanceRestClient") RestClient restClient) {
        super(restClient);
    }

    public List<TokenContractView> listContracts() {
        try {
            SpringPage<TokenContractView> page = get(
                    uriBuilder -> uriBuilder.path("/v1/tokens").queryParam("size", 100).build(),
                    new ParameterizedTypeReference<>() {
                    },
                    DownstreamServices.TOKEN_ISSUANCE);
            return page != null ? page.content() : List.of();
        } catch (ValidationException ex) {
            log.warn("Token Issuance unavailable — skipping contract list: {}", ex.getMessage());
            return List.of();
        }
    }

    public List<TokenHolderView> listHolders(UUID contractId) {
        try {
            SpringPage<TokenHolderView> page = get(
                    uriBuilder -> uriBuilder
                            .path("/v1/tokens/{contractId}/holders")
                            .queryParam("size", 200)
                            .build(contractId),
                    new ParameterizedTypeReference<>() {
                    },
                    DownstreamServices.TOKEN_ISSUANCE);
            return page != null ? page.content() : List.of();
        } catch (ValidationException ex) {
            log.warn("Token Issuance unavailable — skipping holders for {}: {}", contractId, ex.getMessage());
            return List.of();
        }
    }

    /**
     * Stub — syncs holder balance in Issuance from on-chain value during admin remediation.
     */
    public void syncHolderBalance(UUID contractId, String walletAddress, long chainBalance) {
        try {
            postVoid(
                    "/v1/tokens/{contractId}/holders/by-wallet/{walletAddress}/sync-balance",
                    new SyncHolderBalanceRequest(chainBalance),
                    DownstreamServices.TOKEN_ISSUANCE,
                    java.util.Map.of(),
                    contractId,
                    walletAddress);
            log.info("Requested holder balance sync contract={} wallet={} chainBalance={}",
                    contractId, walletAddress, chainBalance);
        } catch (ValidationException ex) {
            log.warn("Token Issuance balance sync unavailable — stub skipped: {}", ex.getMessage());
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

    public record SyncHolderBalanceRequest(long chainBalance) {
    }
}
