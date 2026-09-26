package com.tokenrealty.issuance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "token_holders",
        uniqueConstraints = @UniqueConstraint(columnNames = {"token_contract_id", "investor_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenHolder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "token_contract_id", nullable = false)
    private TokenContract tokenContract;

    // Cross-service reference — investor from Auth/KYC service
    @Column(name = "investor_id", nullable = false)
    private UUID investorId;

    @Column(name = "wallet_address", nullable = false)
    private String walletAddress;

    @Column(name = "balance", nullable = false)
    private Long balance;   // number of tokens held

    @Column(name = "balance_usd", precision = 18, scale = 2)
    private BigDecimal balanceUsd;  // balance * tokenPriceUsd (cached for display)

    @Column(name = "ownership_percentage", precision = 8, scale = 4)
    private BigDecimal ownershipPercentage;  // balance / totalSupply * 100

    @Column(name = "kyc_verified", nullable = false)
    @Builder.Default
    private Boolean kycVerified = false;

    @Column(name = "whitelisted_on_chain", nullable = false)
    @Builder.Default
    private Boolean whitelistedOnChain = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private HolderStatus status = HolderStatus.ACTIVE;

    public enum HolderStatus {
        ACTIVE,
        SUSPENDED,
        EXITED   // balance = 0, fully sold out
    }
}