package com.tokenrealty.integration.service;

import com.tokenrealty.integration.entity.IntegrationDelivery;
import com.tokenrealty.integration.entity.IntegrationDeliveryStatus;
import com.tokenrealty.integration.repository.IntegrationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IntegrationDeliveryRetryWorker {

    private static final List<IntegrationDeliveryStatus> RETRY_STATUSES = List.of(
            IntegrationDeliveryStatus.PENDING,
            IntegrationDeliveryStatus.FAILED);

    private final IntegrationDeliveryRepository deliveryRepository;
    private final WebhookRelayService webhookRelayService;
    private final Clock clock;

    @Value("${tokenrealty.integration.retry.max-attempts:5}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${tokenrealty.integration.retry.poll-ms:5000}")
    public void retryPendingDeliveries() {
        List<IntegrationDelivery> candidates = deliveryRepository.findRetryCandidates(
                RETRY_STATUSES,
                clock.instant(),
                maxAttempts,
                PageRequest.of(0, 20));
        candidates.forEach(this::retryOne);
    }

    private void retryOne(IntegrationDelivery delivery) {
        try {
            webhookRelayService.relayDelivery(delivery.getId());
        } catch (Exception ex) {
            log.warn("Retry worker failed for delivery {}: {}", delivery.getId(), ex.getMessage());
        }
    }
}
