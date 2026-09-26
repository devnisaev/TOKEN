package com.tokenrealty.integration.dto;

import com.tokenrealty.integration.entity.IntegrationDeliveryStatus;
import com.tokenrealty.integration.entity.IntegrationType;

import java.time.Instant;
import java.util.UUID;

public final class IntegrationDtos {

    private IntegrationDtos() {
    }

    public record IntegrationDeliveryView(
            UUID id,
            IntegrationType integrationType,
            String provider,
            String payload,
            IntegrationDeliveryStatus status,
            int attempts,
            Instant nextRetryAt,
            String lastError,
            Instant createdAt
    ) {
    }
}
