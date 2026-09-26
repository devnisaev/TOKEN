package com.tokenrealty.gateway.client;

import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class WalletClient {

    private final RestClient restClient;

    public WalletClient(@Qualifier("walletRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public AggregateBalanceView getAggregateBalance(UUID investorId) {
        try {
            return restClient.get()
                    .uri("/v1/wallets/{investorId}/balance", investorId)
                    .retrieve()
                    .body(AggregateBalanceView.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Investor wallet not found: " + investorId);
            }
            throw unavailable(ex);
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Wallet service unavailable");
        }
    }

    private static ValidationException unavailable(RestClientResponseException ex) {
        if (ex.getStatusCode().is5xxServerError()) {
            return new ValidationException("Wallet service unavailable");
        }
        return new ValidationException("Wallet request failed: " + ex.getStatusText());
    }

    public record FiatBalanceView(String currency, BigDecimal available, BigDecimal held) {
    }

    public record TokenHoldingView(UUID contractId, String tokenSymbol, String walletAddress, long balance) {
    }

    public record AggregateBalanceView(
            UUID investorId,
            String primaryWalletAddress,
            List<FiatBalanceView> fiatBalances,
            List<TokenHoldingView> tokenHoldings) {
    }
}
