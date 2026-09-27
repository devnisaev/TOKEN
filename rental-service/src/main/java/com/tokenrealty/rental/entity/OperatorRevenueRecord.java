package com.tokenrealty.rental.entity;

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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "operator_revenue_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorRevenueRecord extends BaseEntity {

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "gross_revenue_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossRevenueUsd;

    @Column(name = "operator_name", length = 200)
    private String operatorName;

    @Column(name = "source_provider", length = 80)
    private String sourceProvider;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;
}
