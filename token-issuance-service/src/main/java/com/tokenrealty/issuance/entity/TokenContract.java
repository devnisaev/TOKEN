package com.tokenrealty.issuance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "token_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenContract extends BaseEntity {

    // Reference to Property Registry (cross-service — stored as UUID, not FK)
    @Column(name = "flat_id", nullable = false, unique = true)
    private UUID flatId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "spv_wallet_address", nullable = false)
    private String spvWalletAddress;

    /** On-chain DividendDistributor contract — optional until deployed. */
    @Column(name = "dividend_distributor_address", length = 66)
    private String dividendDistributorAddress;

    // On-chain details
    @Column(name = "contract_address", unique = true)
    private String contractAddress;

    @Column(name = "deployment_tx_hash")
    private String deploymentTxHash;

    @Column(name = "deployed_at")
    private Instant deployedAt;

    @Column(name = "total_supply", nullable = false)
    private Long totalSupply;

    @Column(name = "token_price_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal tokenPriceUsd;

    @Column(name = "token_symbol", nullable = false, length = 20)
    private String tokenSymbol;   // e.g. "BKCP-101" (Bishkek City Plaza flat 101)

    @Column(name = "token_name", nullable = false)
    private String tokenName;     // e.g. "Bishkek City Plaza — Flat 101"

    @Column(name = "network", nullable = false)
    private String network;       // "hardhat", "polygon-amoy", "polygon"

    @Column(name = "chain_id", nullable = false)
    private Long chainId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ContractStatus status = ContractStatus.PENDING;

    @OneToMany(mappedBy = "tokenContract", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TokenHolder> holders = new ArrayList<>();

    @OneToMany(mappedBy = "tokenContract", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TokenTransfer> transfers = new ArrayList<>();

    @OneToMany(mappedBy = "tokenContract", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DividendPayment> dividendPayments = new ArrayList<>();

    public enum ContractStatus {
        PENDING,      // Issuance requested, not yet deployed
        DEPLOYING,    // Tx submitted, awaiting confirmation
        ACTIVE,       // Deployed and trading enabled
        SUSPENDED,    // Trading paused
        REDEEMED      // All tokens burned, flat exited
    }
}
