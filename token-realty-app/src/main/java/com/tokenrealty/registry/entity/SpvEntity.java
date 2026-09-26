package com.tokenrealty.registry.entity;

import jakarta.persistence.*;
import lombok.*;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "spv_entities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpvEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false, unique = true)
    private Building building;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "registration_number", unique = true)
    private String registrationNumber;

    @Column(name = "registration_country")
    private String registrationCountry;

    @Column(name = "registration_date")
    private java.time.LocalDate registrationDate;

    @Column(name = "registered_address")
    private String registeredAddress;

    // Wallet address of the SPV on-chain — owns the token contracts
    @Column(name = "wallet_address")
    private String walletAddress;

    @Column(name = "tax_id")
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SpvStatus status = SpvStatus.PENDING;

    @Column(name = "kyc_verified")
    @Builder.Default
    private Boolean kycVerified = false;

    public enum SpvStatus {
        PENDING,
        ACTIVE,
        DISSOLVED
    }
}