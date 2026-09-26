package com.tokenrealty.integration.service;

import com.tokenrealty.integration.client.ComplianceClient;
import com.tokenrealty.integration.dto.IntegrationDtos.IntegrationDeliveryView;
import com.tokenrealty.integration.entity.IntegrationDelivery;
import com.tokenrealty.integration.entity.IntegrationDeliveryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookRelayService {

    private final IntegrationDeliveryService deliveryService;
    private final ComplianceClient complianceClient;

    @Value("${tokenrealty.integration.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${tokenrealty.integration.retry.base-backoff-seconds:30}")
    private long baseBackoffSeconds;

    public void acceptKycWebhook(
            String provider,
            String rawBody,
            String payloadDigest,
            String signature) {
        UUID deliveryId = deliveryService.createPendingKycDelivery(
                provider, rawBody, payloadDigest, signature);
        relayDelivery(deliveryId);
    }

    public Page<IntegrationDeliveryView> listDeliveries(Pageable pageable) {
        return deliveryService.listDeliveries(pageable);
    }

    public void relayDelivery(UUID deliveryId) {
        IntegrationDelivery delivery = deliveryService.requireById(deliveryId);
        if (delivery.getStatus() == IntegrationDeliveryStatus.DELIVERED) {
            return;
        }
        if (delivery.getAttempts() >= maxAttempts) {
            return;
        }

        try {
            complianceClient.forwardKycWebhook(
                    delivery.getProvider(),
                    delivery.getPayload(),
                    delivery.getPayloadDigest(),
                    delivery.getSignature());
            deliveryService.markDelivered(deliveryId);
            log.info("Delivered integration webhook id={} provider={}", deliveryId, delivery.getProvider());
        } catch (RuntimeException ex) {
            deliveryService.markFailed(deliveryId, ex, maxAttempts, baseBackoffSeconds);
            log.warn(
                    "Integration delivery id={} failed: {}",
                    deliveryId,
                    ex.getMessage());
        }
    }
}
