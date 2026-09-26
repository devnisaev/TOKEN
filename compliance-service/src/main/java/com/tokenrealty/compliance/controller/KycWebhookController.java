package com.tokenrealty.compliance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.compliance.dto.ComplianceDtos.KycWebhookPayload;
import com.tokenrealty.compliance.service.KycWebhookService;
import com.tokenrealty.compliance.service.KycWebhookSignatureVerifier;
import com.tokenrealty.web.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/compliance/webhooks/kyc")
@RequiredArgsConstructor
public class KycWebhookController {

    private final KycWebhookService kycWebhookService;
    private final KycWebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping("/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handleKycWebhook(@PathVariable String provider,
                                 @RequestBody String rawBody,
                                 @RequestHeader(value = "X-Payload-Digest", required = false) String payloadDigest,
                                 @RequestHeader(value = "X-Signature", required = false) String signature)
            throws IOException {
        signatureVerifier.verifyIfConfigured(rawBody, payloadDigest, signature);
        KycWebhookPayload payload = objectMapper.readValue(rawBody, KycWebhookPayload.class);
        validatePayload(payload);
        kycWebhookService.handleWebhook(provider, payload);
    }

    private void validatePayload(KycWebhookPayload payload) {
        Set<ConstraintViolation<KycWebhookPayload>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new ValidationException(message);
        }
    }
}
