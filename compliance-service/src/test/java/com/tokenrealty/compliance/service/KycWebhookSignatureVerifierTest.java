package com.tokenrealty.compliance.service;

import com.tokenrealty.compliance.config.OnfidoProperties;
import com.tokenrealty.web.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("KycWebhookSignatureVerifier unit tests")
class KycWebhookSignatureVerifierTest {

    @Test
    void onfidoProvider_usesOnfidoSecretWhenConfigured() {
        OnfidoProperties onfido = new OnfidoProperties();
        onfido.setWebhookSecret("onfido-secret");
        KycWebhookSignatureVerifier verifier = new KycWebhookSignatureVerifier("", onfido);

        assertThat(verifier.isVerificationEnabled("onfido")).isTrue();
        String digest = verifier.computeHmacSha256Hex("onfido", "{\"status\":\"complete\"}");
        assertThat(digest).isNotBlank();

        verifier.verifyIfConfigured("onfido", "{\"status\":\"complete\"}", digest, null);
    }

    @Test
    void sumsubProvider_rejectsInvalidDigest() {
        OnfidoProperties onfido = new OnfidoProperties();
        KycWebhookSignatureVerifier verifier = new KycWebhookSignatureVerifier("sumsub-secret", onfido);

        assertThatThrownBy(() -> verifier.verifyIfConfigured(
                "sumsub", "{}", "bad-digest", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid webhook signature");
    }
}
