package com.tokenrealty.reporting.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tax_summary_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxSummaryRecord extends BaseEntity {

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "payout_id", nullable = false)
    private UUID payoutId;

    @Column(name = "recipient_investor_id", nullable = false)
    private UUID recipientInvestorId;

    @Column(name = "gross_amount_usd", precision = 18, scale = 2)
    private BigDecimal grossAmountUsd;

    @Column(name = "withholding_amount_usd", precision = 18, scale = 2)
    private BigDecimal withholdingAmountUsd;

    @Column(name = "net_amount_usd", precision = 18, scale = 2)
    private BigDecimal netAmountUsd;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;
}
