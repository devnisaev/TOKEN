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
@Table(name = "dividend_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DividendRecord extends BaseEntity {

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(name = "flat_id")
    private UUID flatId;

    @Column(name = "period", length = 20)
    private String period;

    @Column(name = "total_amount_usd", precision = 18, scale = 2)
    private BigDecimal totalAmountUsd;

    @Column(name = "holder_count")
    private Integer holderCount;

    @Column(name = "distributed_at", nullable = false)
    private Instant distributedAt;
}
