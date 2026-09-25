package com.tokenrealty.issuance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "token_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenTransfer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "token_contract_id", nullable = false)
    private TokenContract tokenContract;

    @Column(name = "from_address")
    private String fromAddress;   // null for mint (initial issuance)

    @Column(name = "to_address", nullable = false)
    private String toAddress;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "price_per_token_usd", precision = 18, scale = 2)
    private BigDecimal pricePerTokenUsd;

    @Column(name = "total_value_usd", precision = 18, scale = 2)
    private BigDecimal totalValueUsd;

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransferType type = TransferType.TRANSFER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name = "failure_reason")
    private String failureReason;

    public enum TransferType {
        MINT,      // Initial token issuance to SPV wallet
        TRANSFER,  // Investor-to-investor
        BURN       // Token redemption
    }

    public enum TransferStatus {
        PENDING,    // Submitted to mempool
        CONFIRMED,  // Mined and confirmed
        FAILED      // Reverted or timed out
    }
}
