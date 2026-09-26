package com.tokenrealty.rental.client;

import com.tokenrealty.web.exception.ValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(@Qualifier("paymentRestClient") RestClient restClient) {
        this.restClient = restClient;
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
        try {
            return restClient.post()
                    .uri("/v1/payouts")
                    .body(body)
                    .retrieve()
                    .body(PayoutResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ValidationException("Payment service unavailable");
            }
            throw new ValidationException("Rent collection failed: " + ex.getStatusText());
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Payment service unavailable");
        }
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
