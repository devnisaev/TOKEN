package com.tokenrealty.payment.integration;

import com.tokenrealty.payment.dto.PaymentDtos.InitiatePaymentRequest;
import com.tokenrealty.payment.entity.Payment;
import com.tokenrealty.payment.entity.PaymentCurrency;
import com.tokenrealty.payment.service.PaymentService;
import com.tokenrealty.payment.service.PaymentWebhookService;
import com.tokenrealty.web.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "tokenrealty.payment.webhook.stripe-secret=test-stripe-secret")
class PaymentWebhookIntegrationTest {

    @Autowired PaymentWebhookService paymentWebhookService;
    @Autowired PaymentService paymentService;

    @Test
    @DisplayName("valid webhook confirms pending payment")
    void validWebhook_confirmsPayment() throws Exception {
        var initiated = paymentService.initiate(InitiatePaymentRequest.builder()
                .orderId(UUID.randomUUID())
                .payerId(UUID.randomUUID())
                .payerWallet("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266")
                .amount(new BigDecimal("100.00"))
                .currency(PaymentCurrency.USDC)
                .paymentType(Payment.PaymentType.TOKEN_PURCHASE)
                .build(), "webhook-" + UUID.randomUUID());

        String body = "{\"paymentId\":\"" + initiated.id() + "\",\"txHash\":\"0xabc123def456789012345678901234567890123456789012345678901234\"}";
        paymentWebhookService.handleWebhook("stripe", body, null, hmac(body));

        assertThat(paymentService.findById(initiated.id()).status())
                .isEqualTo(Payment.PaymentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("invalid signature is rejected when secret configured")
    void invalidSignature_rejected() throws Exception {
        var initiated = paymentService.initiate(InitiatePaymentRequest.builder()
                .orderId(UUID.randomUUID())
                .payerId(UUID.randomUUID())
                .payerWallet("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266")
                .amount(new BigDecimal("50.00"))
                .currency(PaymentCurrency.USDC)
                .paymentType(Payment.PaymentType.TOKEN_PURCHASE)
                .build(), "webhook-invalid-" + UUID.randomUUID());

        String body = "{\"paymentId\":\"" + initiated.id() + "\",\"txHash\":\"0xabc123def456789012345678901234567890123456789012345678901234\"}";

        assertThatThrownBy(() -> paymentWebhookService.handleWebhook(
                "stripe", body, null, "bad-signature"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid payment webhook signature");
    }

    private String hmac(String body) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    "test-stripe-secret".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
