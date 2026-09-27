package com.tokenrealty.corporateactions.dto;

import com.tokenrealty.corporateactions.entity.CorporateAction;
import com.tokenrealty.corporateactions.entity.CorporateActionStatus;
import com.tokenrealty.corporateactions.entity.CorporateActionType;
import com.tokenrealty.corporateactions.entity.IndexDefinition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CorporateActionDtos {

    private CorporateActionDtos() {
    }

    public record CorporateActionView(
            UUID id,
            CorporateActionType type,
            UUID flatId,
            UUID contractId,
            String period,
            BigDecimal grossAmountUsd,
            BigDecimal splitRatio,
            CorporateActionStatus status,
            UUID sourceEventId,
            Instant createdAt
    ) {
        public static CorporateActionView from(CorporateAction action) {
            return new CorporateActionView(
                    action.getId(),
                    action.getType(),
                    action.getFlatId(),
                    action.getContractId(),
                    action.getPeriod(),
                    action.getGrossAmountUsd(),
                    action.getSplitRatio(),
                    action.getStatus(),
                    action.getSourceEventId(),
                    action.getCreatedAt()
            );
        }
    }

    public record RequestStockSplitRequest(
            @NotNull UUID flatId,
            @NotNull @DecimalMin("1.0001") BigDecimal splitRatio,
            @NotBlank @Size(max = 10) String period
    ) {
    }

    public record IndexConstituentRequest(
            @NotNull UUID contractId,
            @Min(1) @Max(10000) int weightBps
    ) {
    }

    public record IndexConstituentResponse(
            UUID contractId,
            int weightBps
    ) {
    }

    @Builder
    public record CreateIndexDefinitionRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 20) String symbol,
            @Size(max = 500) String description,
            UUID indexContractId,
            @NotEmpty List<@Valid IndexConstituentRequest> constituents
    ) {
    }

    public record RebalanceIndexRequest(
            @NotEmpty List<@Valid IndexConstituentRequest> constituents
    ) {
    }

    @Builder
    public record IndexDefinitionResponse(
            UUID id,
            String name,
            String symbol,
            String description,
            UUID indexContractId,
            IndexDefinition.IndexStatus status,
            List<IndexConstituentResponse> constituents,
            Instant createdAt
    ) {
    }
}
