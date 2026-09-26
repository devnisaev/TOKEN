package com.tokenrealty.corporateactions.dto;

import com.tokenrealty.corporateactions.entity.CorporateAction;
import com.tokenrealty.corporateactions.entity.CorporateActionStatus;
import com.tokenrealty.corporateactions.entity.CorporateActionType;

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
                    action.getStatus(),
                    action.getSourceEventId(),
                    action.getCreatedAt()
            );
        }
    }
}
