package com.tokenrealty.integration.service;

import com.tokenrealty.integration.dto.IntegrationDtos.IntegrationDeliveryView;
import com.tokenrealty.integration.entity.IntegrationDelivery;
import com.tokenrealty.integration.entity.IntegrationDeliveryStatus;
import com.tokenrealty.integration.entity.IntegrationType;
import com.tokenrealty.integration.repository.IntegrationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntegrationDeliveryService {

    private final IntegrationDeliveryRepository deliveryRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public IntegrationDelivery requireById(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public Page<IntegrationDeliveryView> listDeliveries(Pageable pageable) {
        return deliveryRepository.findAll(pageable).map(this::toView);
    }

    @Transactional
    public UUID createPendingKycDelivery(
            String provider,
            String rawBody,
            String payloadDigest,
            String signature) {
        IntegrationDelivery delivery = IntegrationDelivery.builder()
                .integrationType(IntegrationType.KYC)
                .provider(provider)
                .payload(rawBody)
                .payloadDigest(payloadDigest)
                .signature(signature)
                .status(IntegrationDeliveryStatus.PENDING)
                .attempts(0)
                .build();
        return deliveryRepository.save(delivery).getId();
    }

    @Transactional
    public void markDelivered(UUID deliveryId) {
        IntegrationDelivery delivery = requireById(deliveryId);
        delivery.setStatus(IntegrationDeliveryStatus.DELIVERED);
        delivery.setLastError(null);
        delivery.setNextRetryAt(null);
    }

    @Transactional
    public void markFailed(UUID deliveryId, RuntimeException ex, int maxAttempts, long baseBackoffSeconds) {
        IntegrationDelivery delivery = requireById(deliveryId);
        int attempt = delivery.getAttempts() + 1;
        delivery.setAttempts(attempt);
        delivery.setLastError(truncate(ex.getMessage()));
        delivery.setStatus(IntegrationDeliveryStatus.FAILED);
        if (attempt >= maxAttempts) {
            delivery.setNextRetryAt(null);
            return;
        }
        delivery.setNextRetryAt(clock.instant().plus(backoff(attempt, baseBackoffSeconds)));
    }

    private Duration backoff(int attempt, long baseBackoffSeconds) {
        long multiplier = 1L << Math.max(0, attempt - 1);
        return Duration.ofSeconds(baseBackoffSeconds * multiplier);
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private IntegrationDeliveryView toView(IntegrationDelivery delivery) {
        return new IntegrationDeliveryView(
                delivery.getId(),
                delivery.getIntegrationType(),
                delivery.getProvider(),
                delivery.getPayload(),
                delivery.getStatus(),
                delivery.getAttempts(),
                delivery.getNextRetryAt(),
                delivery.getLastError(),
                delivery.getCreatedAt()
        );
    }
}
