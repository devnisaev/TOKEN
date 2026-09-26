package com.tokenrealty.wallet.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "investor_wallets", indexes = {
        @Index(name = "idx_investor_wallets_investor", columnList = "investor_id"),
        @Index(name = "uk_investor_wallets_address", columnList = "wallet_address", unique = true)
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InvestorWallet extends BaseEntity {

    @Column(name = "investor_id", nullable = false)
    private UUID investorId;

    @Column(name = "wallet_address", nullable = false, length = 66)
    private String walletAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false, length = 20)
    private WalletType walletType;

    /** AES-GCM encrypted hex private key — custodial wallets only; never log or expose. */
    @Column(name = "encrypted_private_key", columnDefinition = "TEXT")
    private String encryptedPrivateKey;

    @Column(length = 120)
    private String label;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

    public enum WalletType {
        CUSTODIAL,
        LINKED
    }
}
