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
@Table(name = "stock_split_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockSplitRecord extends BaseEntity {

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "corporate_action_id", nullable = false)
    private UUID corporateActionId;

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(name = "period", nullable = false, length = 32)
    private String period;

    @Column(name = "split_ratio", nullable = false, precision = 18, scale = 4)
    private BigDecimal splitRatio;

    @Column(name = "new_total_supply")
    private Long newTotalSupply;

    @Column(name = "new_token_price_usd", precision = 18, scale = 2)
    private BigDecimal newTokenPriceUsd;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;
}
