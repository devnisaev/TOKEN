package com.tokenrealty.marketplace.entity;

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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rfq_requests", indexes = {
        @Index(name = "idx_rfq_requests_contract", columnList = "contract_id"),
        @Index(name = "idx_rfq_requests_requester", columnList = "requester_id"),
        @Index(name = "idx_rfq_requests_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RfqRequest extends BaseEntity {

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RfqSide side;

    @Column(name = "token_amount", nullable = false)
    private long tokenAmount;

    @Column(name = "notional_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal notionalUsd;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "wallet_address", nullable = false, length = 66)
    private String walletAddress;

    @Column(name = "liquidity_tier", nullable = false, length = 10)
    @Builder.Default
    private String liquidityTier = "TIER_2";

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RfqStatus status = RfqStatus.OPEN;

    @Column(name = "accepted_quote_id")
    private UUID acceptedQuoteId;

    public enum RfqSide {
        BUY, SELL
    }

    public enum RfqStatus {
        OPEN, QUOTED, ACCEPTED, EXPIRED, CANCELLED
    }
}
