package com.tokenrealty.compliance.service;

import com.tokenrealty.web.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class KycWebhookSignatureVerifier {

    private String webhookSecret;

    public KycWebhookSignatureVerifier(
            @Value("${tokenrealty.compliance.sumsub.webhook-secret:}") String webhookSecret) {
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
    }

    public boolean isVerificationEnabled() {
        return !webhookSecret.isBlank();
    }

    public void verifyIfConfigured(String rawBody, String payloadDigest, String signature) {
        if (!isVerificationEnabled()) {
            return;
        }
        String provided = resolveProvidedSignature(payloadDigest, signature);
        if (provided == null || provided.isBlank()) {
            raiseValidation("Webhook signature header required");
        }
        String expected = computeHmacSha256Hex(rawBody);
        if (!MessageDigest.isEqual(
                provided.toLowerCase().getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8))) {
            raiseValidation("Invalid webhook signature");
        }
    }

    public String computeHmacSha256Hex(String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute webhook HMAC", ex);
        }
    }

    private static String resolveProvidedSignature(String payloadDigest, String signature) {
        if (payloadDigest != null && !payloadDigest.isBlank()) {
            return payloadDigest.trim();
        }
        if (signature != null && !signature.isBlank()) {
            return signature.trim();
        }
        return null;
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
