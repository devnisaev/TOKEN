package com.tokenrealty.marketplace.client;

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

    public InitiatePaymentResponse initiateTokenPurchase(
            UUID orderId,
            UUID payerId,
            String payerWallet,
            BigDecimal amountUsd
    ) {
        return initiateTokenPurchase(orderId, payerId, payerWallet, amountUsd, null);
    }

    public InitiatePaymentResponse initiateTokenPurchase(
            UUID orderId,
            UUID payerId,
            String payerWallet,
            BigDecimal amountUsd,
            UUID sellerRecipientId
    ) {
        InitiatePaymentRequest body = new InitiatePaymentRequest(
                orderId, payerId, payerWallet, amountUsd, "USDC", "TOKEN_PURCHASE", sellerRecipientId);
        return post(
                "/v1/payments",
                body,
                InitiatePaymentResponse.class,
                DownstreamServices.PAYMENT,
                RestClientOperations.idempotencyKey("marketplace-order-" + orderId));
    }

    public void releaseEscrow(UUID paymentId) {
        patchVoid("/v1/payments/{id}/release", DownstreamServices.PAYMENT, paymentId);
    }

    public record InitiatePaymentRequest(
            UUID orderId,
            UUID payerId,
            String payerWallet,
            BigDecimal amount,
            String currency,
            String paymentType,
            UUID sellerRecipientId
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
}
