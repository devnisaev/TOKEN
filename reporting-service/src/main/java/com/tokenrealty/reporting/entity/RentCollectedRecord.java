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
@Table(name = "rent_collected_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentCollectedRecord extends BaseEntity {

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "lease_id")
    private UUID leaseId;

    @Column(name = "flat_id")
    private UUID flatId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "period", length = 20)
    private String period;

    @Column(name = "amount_usd", precision = 18, scale = 2)
    private BigDecimal amountUsd;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;
}
