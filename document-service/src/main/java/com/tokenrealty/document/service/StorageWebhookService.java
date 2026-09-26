package com.tokenrealty.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.document.dto.StorageWebhookPayload;
import com.tokenrealty.document.entity.StorageWebhookEvent;
import com.tokenrealty.document.repository.StorageWebhookEventRepository;
import com.tokenrealty.web.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageWebhookService {

    private final StorageWebhookEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Clock clock;

    @Transactional
    public void handleWebhook(
            String provider,
            String rawBody,
            String payloadDigest,
            String signature) throws IOException {
        if (payloadDigest != null && repository.findByPayloadDigest(payloadDigest).isPresent()) {
            return;
        }
        StorageWebhookPayload payload = objectMapper.readValue(rawBody, StorageWebhookPayload.class);
        validatePayload(payload);
        repository.save(StorageWebhookEvent.builder()
                .provider(provider)
                .eventType(payload.event())
                .objectKey(payload.objectKey())
                .ipfsCid(payload.ipfsCid())
                .payloadDigest(payloadDigest)
                .processedAt(clock.instant())
                .build());
        log.info(
                "Processed storage webhook provider={} event={} objectKey={} ipfsCid={}",
                provider,
                payload.event(),
                payload.objectKey(),
                payload.ipfsCid());
    }

    private void validatePayload(StorageWebhookPayload payload) {
        Set<ConstraintViolation<StorageWebhookPayload>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new ValidationException(message);
        }
    }
}
