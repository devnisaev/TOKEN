package com.tokenrealty.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.payment.dto.PaymentDtos.ConfirmPaymentRequest;
import com.tokenrealty.payment.dto.PaymentWebhookPayload;
import com.tokenrealty.web.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public void handleWebhook(String provider, String rawBody) throws IOException {
        PaymentWebhookPayload payload = objectMapper.readValue(rawBody, PaymentWebhookPayload.class);
        validatePayload(payload);
        paymentService.confirm(
                payload.paymentId(),
                ConfirmPaymentRequest.builder().txHash(payload.txHash()).build());
    }

    private void validatePayload(PaymentWebhookPayload payload) {
        Set<ConstraintViolation<PaymentWebhookPayload>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new ValidationException("Invalid payment webhook payload: " + message);
        }
    }
}
