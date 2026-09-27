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
import java.util.UUID;

@Entity
@Table(name = "rfq_quotes", indexes = {
        @Index(name = "idx_rfq_quotes_request", columnList = "rfq_request_id"),
        @Index(name = "idx_rfq_quotes_quoter", columnList = "quoter_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RfqQuote extends BaseEntity {

    @Column(name = "rfq_request_id", nullable = false)
    private UUID rfqRequestId;

    @Column(name = "quoter_id", nullable = false)
    private UUID quoterId;

    @Column(name = "wallet_address", nullable = false, length = 66)
    private String walletAddress;

    @Column(name = "price_per_token_usd", nullable = false, precision = 19, scale = 8)
    private BigDecimal pricePerTokenUsd;

    @Column(name = "total_price_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPriceUsd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private QuoteStatus status = QuoteStatus.PENDING;

    public enum QuoteStatus {
        PENDING, ACCEPTED, REJECTED, EXPIRED
    }
}
