package com.tokenrealty.gateway.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class TokenIssuanceClient extends DownstreamRestClientSupport {

    public TokenIssuanceClient(@Qualifier("tokenIssuanceRestClient") RestClient restClient) {
        super(restClient);
    }

    public List<DividendPaymentView> listInvestorDividends(UUID investorId) {
        DividendPaymentView[] body = get(
                "/v1/investors/{investorId}/dividends",
                DividendPaymentView[].class,
                DownstreamServices.TOKEN_ISSUANCE,
                investorId);
        return body == null ? List.of() : List.of(body);
    }

    public TokenContractView getContractByFlatId(UUID flatId) {
        return getAllowNotFound(
                "/v1/tokens/by-flat/{flatId}",
                TokenContractView.class,
                DownstreamServices.TOKEN_ISSUANCE,
                flatId);
    }

    public record DividendPaymentView(
            UUID id,
            UUID contractId,
            UUID investorId,
            String investorWallet,
            LocalDate periodStart,
            LocalDate periodEnd,
            Long tokensHeld,
            BigDecimal ownershipPct,
            BigDecimal grossRentalIncomeUsd,
            BigDecimal amountUsd,
            String txHash,
            Instant paidAt,
            String status,
            Instant createdAt) {
    }

    public record TokenContractView(
            UUID id,
            UUID flatId,
            String contractAddress,
            String tokenSymbol,
            Long totalSupply,
            BigDecimal tokenPriceUsd,
            String status
    ) {
    }
}
