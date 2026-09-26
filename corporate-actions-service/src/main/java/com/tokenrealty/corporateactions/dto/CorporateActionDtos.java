package com.tokenrealty.corporateactions.dto;

import com.tokenrealty.corporateactions.entity.CorporateAction;
import com.tokenrealty.corporateactions.entity.CorporateActionStatus;
import com.tokenrealty.corporateactions.entity.CorporateActionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
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
}
