package com.tokenrealty.issuance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "compliance_records",
        uniqueConstraints = @UniqueConstraint(columnNames = "wallet_address"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceRecord extends BaseEntity {

    @Column(name = "investor_id", nullable = false)
    private UUID investorId;

    @Column(name = "wallet_address", nullable = false, unique = true)
    private String walletAddress;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "country_code", length = 2)
    private String countryCode;   // ISO 3166-1 alpha-2

    @Column(name = "kyc_provider")
    private String kycProvider;   // e.g. "Sumsub", "Onfido"

    @Column(name = "kyc_reference_id")
    private String kycReferenceId;

    @Column(name = "kyc_verified_at")
    private Instant kycVerifiedAt;

    @Column(name = "kyc_expires_at")
    private Instant kycExpiresAt;

    // Whether this wallet is whitelisted in the on-chain ComplianceRegistry contract
    @Column(name = "on_chain_whitelisted", nullable = false)
    @Builder.Default
    private Boolean onChainWhitelisted = false;

    @Column(name = "whitelist_tx_hash")
    private String whitelistTxHash;

    @Column(name = "whitelisted_at")
    private Instant whitelistedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ComplianceStatus status = ComplianceStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    public enum ComplianceStatus {
        PENDING,     // KYC submitted, not yet reviewed
        APPROVED,    // KYC passed, wallet whitelisted
        REJECTED,    // KYC failed
        EXPIRED,     // KYC valid period elapsed — needs renewal
        REVOKED      // Manually revoked by compliance team
    }
}