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
@Table(name = "flat_tokenized_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlatTokenizedRecord extends BaseEntity {

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "flat_id", nullable = false, unique = true)
    private UUID flatId;

    @Column(name = "building_id")
    private UUID buildingId;

    @Column(name = "contract_address", length = 66)
    private String contractAddress;

    @Column(name = "total_tokens")
    private Long totalTokens;

    @Column(name = "token_price_usd", precision = 18, scale = 2)
    private BigDecimal tokenPriceUsd;

    @Column(name = "tokenized_at", nullable = false)
    private Instant tokenizedAt;
}
