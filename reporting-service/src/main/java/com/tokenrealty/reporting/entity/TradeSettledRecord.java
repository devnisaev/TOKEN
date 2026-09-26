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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trade_settled_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeSettledRecord extends BaseEntity {

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "trade_id", nullable = false)
    private UUID tradeId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "listing_id")
    private UUID listingId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;
}
