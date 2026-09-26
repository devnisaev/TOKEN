package com.tokenrealty.integration.service;

import com.tokenrealty.integration.client.ComplianceClient;
import com.tokenrealty.integration.client.DocumentClient;
import com.tokenrealty.integration.client.PaymentClient;
import com.tokenrealty.integration.entity.IntegrationType;
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
    private final DocumentClient documentClient;
    private final PaymentClient paymentClient;

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

    public void acceptDocumentWebhook(
            String provider,
            String rawBody,
            String payloadDigest,
            String signature) {
        UUID deliveryId = deliveryService.createPendingDocumentDelivery(
                provider, rawBody, payloadDigest, signature);
        relayDelivery(deliveryId);
    }

    public void acceptPaymentWebhook(
            String provider,
            String rawBody,
            String payloadDigest,
            String signature) {
        UUID deliveryId = deliveryService.createPendingPaymentDelivery(
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
            relayToDownstream(delivery);
            deliveryService.markDelivered(deliveryId);
            log.info(
                    "Delivered integration webhook id={} type={} provider={}",
                    deliveryId,
                    delivery.getIntegrationType(),
                    delivery.getProvider());
        } catch (RuntimeException ex) {
            deliveryService.markFailed(deliveryId, ex, maxAttempts, baseBackoffSeconds);
            log.warn(
                    "Integration delivery id={} failed: {}",
                    deliveryId,
                    ex.getMessage());
        }
    }

    private void relayToDownstream(IntegrationDelivery delivery) {
        if (delivery.getIntegrationType() == IntegrationType.DOCUMENT) {
            documentClient.forwardStorageWebhook(
                    delivery.getProvider(),
                    delivery.getPayload(),
                    delivery.getPayloadDigest(),
                    delivery.getSignature());
            return;
        }
        if (delivery.getIntegrationType() == IntegrationType.PAYMENT) {
            paymentClient.forwardPaymentWebhook(
                    delivery.getProvider(),
                    delivery.getPayload(),
                    delivery.getPayloadDigest(),
                    delivery.getSignature());
            return;
        }
        complianceClient.forwardKycWebhook(
                delivery.getProvider(),
                delivery.getPayload(),
                delivery.getPayloadDigest(),
                delivery.getSignature());
    }
}
