package com.tokenrealty.integration.dto;

import com.tokenrealty.integration.entity.IntegrationDeliveryStatus;
import com.tokenrealty.integration.entity.IntegrationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    public record IntegrationCredentialView(
            UUID id,
            IntegrationType integrationType,
            String provider,
            int version,
            Instant rotatedAt,
            Instant createdAt
    ) {
    }

    public record RotateCredentialRequest(
            @NotBlank @Size(max = 2000) String secret
    ) {
    }
}
