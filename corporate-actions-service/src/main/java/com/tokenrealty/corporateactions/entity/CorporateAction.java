package com.tokenrealty.corporateactions.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "corporate_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateAction extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CorporateActionType type;

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(nullable = false, length = 10)
    private String period;

    @Column(name = "gross_amount_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmountUsd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CorporateActionStatus status;

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;
}
