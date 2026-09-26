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
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(@Qualifier("paymentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public InitiatePaymentResponse initiateTokenPurchase(
            UUID orderId,
            UUID payerId,
            String payerWallet,
            BigDecimal amountUsd
    ) {
        InitiatePaymentRequest body = new InitiatePaymentRequest(
                orderId, payerId, payerWallet, amountUsd, "USDC", "TOKEN_PURCHASE");
        try {
            return restClient.post()
                    .uri("/v1/payments")
                    .header("Idempotency-Key", "marketplace-order-" + orderId)
                    .body(body)
                    .retrieve()
                    .body(InitiatePaymentResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ValidationException("Payment service unavailable");
            }
            throw new ValidationException("Payment initiation failed: " + ex.getStatusText());
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Payment service unavailable");
        }
    }

    public record InitiatePaymentRequest(
            UUID orderId,
            UUID payerId,
            String payerWallet,
            BigDecimal amount,
            String currency,
            String paymentType
    ) {
    }

    public record InitiatePaymentResponse(
            UUID id,
            UUID orderId,
            EscrowResponse escrow
    ) {
    }

    public record EscrowResponse(
            UUID id,
            String escrowWalletAddress,
            String status
    ) {
    }

    public void releaseEscrow(UUID paymentId) {
        try {
            restClient.patch()
                    .uri("/v1/payments/{id}/release", paymentId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is5xxServerError()) {
                throw new ValidationException("Payment service unavailable");
            }
            throw new ValidationException("Escrow release failed: " + ex.getStatusText());
        } catch (ResourceAccessException ex) {
            throw new ValidationException("Payment service unavailable");
        }
    }
}
