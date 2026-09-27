package com.tokenrealty.integration.dto;

import com.tokenrealty.integration.entity.IntegrationDeliveryStatus;
import com.tokenrealty.integration.entity.IntegrationType;
import com.tokenrealty.integration.entity.BridgeTransfer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

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

    @Builder
    public record CreateBridgeTransferRequest(
            @NotNull UUID sourceContractId,
            @NotBlank @Size(max = 32) String sourceChain,
            @NotBlank @Size(max = 32) String targetChain,
            @NotNull UUID investorId,
            @NotBlank @Size(max = 66) String walletAddress,
            @NotNull @Min(1) Long tokenAmount
    ) {
    }

    @Builder
    public record BridgeTransferResponse(
            UUID id,
            UUID sourceContractId,
            String sourceChain,
            String targetChain,
            UUID investorId,
            String walletAddress,
            long tokenAmount,
            BridgeTransfer.BridgeStatus status,
            String relayTxHash,
            Instant createdAt
    ) {
    }
}
