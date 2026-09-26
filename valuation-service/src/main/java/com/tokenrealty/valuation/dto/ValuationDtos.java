package com.tokenrealty.valuation.dto;

import com.tokenrealty.valuation.entity.ValuationRequestStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ValuationDtos {

    private ValuationDtos() {
    }

    public record SubmitValuationRequest(
            @NotNull UUID buildingId,
            @NotNull UUID flatId,
            @NotNull @DecimalMin("0.01") BigDecimal valueUsd,
            @Min(1) long totalTokens,
            @Size(max = 1000) String notes
    ) {
    }

    public record RejectValuationRequest(
            @Size(max = 1000) String reason
    ) {
    }

    public record ValuationRequestResponse(
            UUID id,
            UUID buildingId,
            UUID flatId,
            BigDecimal valueUsd,
            long totalTokens,
            ValuationRequestStatus status,
            UUID submittedBy,
            UUID reviewedBy,
            String notes,
            String rejectionReason,
            Instant reviewedAt,
            UUID navSnapshotId,
            Instant createdAt
    ) {
    }

    public record NavSnapshotResponse(
            UUID id,
            UUID flatId,
            UUID buildingId,
            UUID valuationRequestId,
            BigDecimal valueUsd,
            long totalTokens,
            BigDecimal navPerTokenUsd,
            Instant approvedAt,
            Instant createdAt
    ) {
    }

    public record CreateRevaluationScheduleRequest(
            @NotNull UUID buildingId,
            UUID flatId,
            @Min(1) @Max(120) int intervalMonths,
            Instant nextDueAt
    ) {
    }

    public record RevaluationScheduleResponse(
            UUID id,
            UUID buildingId,
            UUID flatId,
            int intervalMonths,
            Instant nextDueAt,
            boolean active,
            Instant createdAt
    ) {
    }
}
