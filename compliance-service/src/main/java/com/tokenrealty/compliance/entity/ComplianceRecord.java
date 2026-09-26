package com.tokenrealty.compliance.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

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
    private String countryCode;

    @Column(name = "kyc_provider")
    private String kycProvider;

    @Column(name = "kyc_reference_id")
    private String kycReferenceId;

    @Column(name = "kyc_verified_at")
    private Instant kycVerifiedAt;

    @Column(name = "kyc_expires_at")
    private Instant kycExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ComplianceStatus status = ComplianceStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    public enum ComplianceStatus {
        PENDING,
        APPROVED,
        REJECTED,
        EXPIRED,
        REVOKED
    }
}
