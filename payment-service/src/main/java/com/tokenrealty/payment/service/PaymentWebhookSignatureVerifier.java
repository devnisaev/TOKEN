package com.tokenrealty.payment.service;

import com.tokenrealty.web.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class PaymentWebhookSignatureVerifier {

    private final String stripeWebhookSecret;
    private final String moonpayWebhookSecret;

    public PaymentWebhookSignatureVerifier(
            @Value("${tokenrealty.payment.webhook.stripe-secret:}") String stripeWebhookSecret,
            @Value("${tokenrealty.payment.webhook.moonpay-secret:}") String moonpayWebhookSecret) {
        this.stripeWebhookSecret = trim(stripeWebhookSecret);
        this.moonpayWebhookSecret = trim(moonpayWebhookSecret);
    }

    public void verifyIfConfigured(
            String provider,
            String rawBody,
            String payloadDigest,
            String signature) {
        String secret = resolveSecret(provider);
        if (secret.isBlank()) {
            return;
        }
        String provided = resolveProvidedSignature(payloadDigest, signature);
        if (provided == null || provided.isBlank()) {
            raiseValidation("Payment webhook signature header required");
        }
        String expected = hmacSha256Hex(secret, rawBody);
        if (!MessageDigest.isEqual(
                provided.toLowerCase().getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8))) {
            raiseValidation("Invalid payment webhook signature");
        }
    }

    private String resolveSecret(String provider) {
        if (provider == null) {
            return "";
        }
        return switch (provider.toLowerCase()) {
            case "stripe" -> stripeWebhookSecret;
            case "moonpay" -> moonpayWebhookSecret;
            default -> "";
        };
    }

    private static String resolveProvidedSignature(String payloadDigest, String signature) {
        if (signature != null && !signature.isBlank()) {
            return signature;
        }
        return payloadDigest;
    }

    private static String hmacSha256Hex(String secret, String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ValidationException("Failed to verify payment webhook signature");
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
