package com.tokenrealty.compliance.service;

import com.tokenrealty.compliance.config.OnfidoProperties;
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

    private static final String SUMSUB_PROVIDER = "sumsub";
    private static final String ONFIDO_PROVIDER = "onfido";

    private final String sumsubWebhookSecret;
    private final String onfidoWebhookSecret;

    public KycWebhookSignatureVerifier(
            @Value("${tokenrealty.compliance.sumsub.webhook-secret:}") String sumsubWebhookSecret,
            OnfidoProperties onfidoProperties) {
        this.sumsubWebhookSecret = trim(sumsubWebhookSecret);
        this.onfidoWebhookSecret = trim(onfidoProperties.getWebhookSecret());
    }

    public boolean isVerificationEnabled(String provider) {
        return !resolveSecret(provider).isBlank();
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
            raiseValidation("Webhook signature header required");
        }
        String expected = hmacSha256Hex(secret, rawBody);
        if (!MessageDigest.isEqual(
                provided.toLowerCase().getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8))) {
            raiseValidation("Invalid webhook signature");
        }
    }

    public String computeHmacSha256Hex(String provider, String rawBody) {
        return hmacSha256Hex(resolveSecret(provider), rawBody);
    }

    private static String hmacSha256Hex(String secret, String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute webhook HMAC", ex);
        }
    }

    private String resolveSecret(String provider) {
        if (SUMSUB_PROVIDER.equalsIgnoreCase(provider)) {
            return sumsubWebhookSecret;
        }
        if (ONFIDO_PROVIDER.equalsIgnoreCase(provider)) {
            return onfidoWebhookSecret;
        }
        return "";
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

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
