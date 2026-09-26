package com.tokenrealty.gateway.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class WalletClient extends DownstreamRestClientSupport {

    public WalletClient(@Qualifier("walletRestClient") RestClient restClient) {
        super(restClient);
    }

    public AggregateBalanceView getAggregateBalance(UUID investorId) {
        return get(
                "/v1/wallets/{investorId}/balance",
                AggregateBalanceView.class,
                DownstreamServices.WALLET,
                "Investor wallet not found: " + investorId,
                investorId);
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
