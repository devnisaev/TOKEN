package com.tokenrealty.wallet.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(@Qualifier("paymentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public WalletBalanceResponse getWalletBalance(UUID investorId) {
        try {
            return restClient.get()
                    .uri("/v1/wallet-balances/{investorId}", investorId)
                    .retrieve()
                    .body(WalletBalanceResponse.class);
        } catch (Exception ex) {
            return new WalletBalanceResponse(investorId, "USDC", BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    public record WalletBalanceResponse(
            UUID investorId,
            String currency,
            BigDecimal availableBalance,
            BigDecimal heldBalance
    ) {
    }
}
