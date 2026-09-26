package com.tokenrealty.rental.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import com.tokenrealty.web.rest.RestClientOperations;
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

    public PayoutResponse recordRentCollection(
            UUID leaseId,
            UUID flatId,
            UUID tenantId,
            UUID spvRecipientId,
            String spvWallet,
            BigDecimal amount,
            String period
    ) {
        CreatePayoutRequest body = new CreatePayoutRequest(
                spvRecipientId,
                spvWallet,
                amount,
                "USDC",
                "RENT",
                leaseId,
                flatId,
                tenantId,
                period
        );
        return post(
                "/v1/payouts",
                body,
                PayoutResponse.class,
                DownstreamServices.PAYMENT,
                RestClientOperations.idempotencyKey("rent-" + leaseId + "-" + period));
    }

    public record CreatePayoutRequest(
            UUID recipientInvestorId,
            String recipientWallet,
            BigDecimal amount,
            String currency,
            String purpose,
            UUID referenceId,
            UUID flatId,
            UUID tenantId,
            String period
    ) {
    }

    public record PayoutResponse(UUID id) {
    }
}
