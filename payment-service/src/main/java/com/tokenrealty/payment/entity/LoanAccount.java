package com.tokenrealty.payment.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "loan_accounts", indexes = {
        @Index(name = "idx_loan_investor", columnList = "investor_id"),
        @Index(name = "idx_loan_collateral", columnList = "collateral_position_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanAccount extends BaseEntity {

    @Column(name = "investor_id", nullable = false)
    private UUID investorId;

    @Column(name = "collateral_position_id", nullable = false)
    private UUID collateralPositionId;

    @Column(name = "principal_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalUsd;

    @Column(name = "outstanding_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingUsd;

    @Column(name = "interest_rate_bps", nullable = false)
    @Builder.Default
    private int interestRateBps = 800;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LoanStatus status = LoanStatus.ACTIVE;

    public enum LoanStatus {
        ACTIVE, REPAID, LIQUIDATED
    }
}
