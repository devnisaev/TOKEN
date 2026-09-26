package com.tokenrealty.wallet.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentClient extends DownstreamRestClientSupport {

    public PaymentClient(@Qualifier("paymentRestClient") RestClient restClient) {
        super(restClient);
    }

    public WalletBalanceResponse getWalletBalance(UUID investorId) {
        WalletBalanceResponse response = getAllowNotFound(
                "/v1/wallet-balances/{investorId}",
                WalletBalanceResponse.class,
                DownstreamServices.PAYMENT,
                investorId);
        if (response != null) {
            return response;
        }
        return new WalletBalanceResponse(investorId, "USDC", BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public record WalletBalanceResponse(
            UUID investorId,
            String currency,
            BigDecimal availableBalance,
            BigDecimal heldBalance
    ) {
    }
}
