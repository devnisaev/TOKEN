package com.tokenrealty.integration.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "integration_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationCredential extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false, length = 20)
    private IntegrationType integrationType;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "secret_ciphertext", nullable = false, length = 4000)
    private String secretCiphertext;

    @Column(name = "credential_version", nullable = false)
    private int credentialVersion;

    @Column(name = "rotated_at", nullable = false)
    private Instant rotatedAt;
}
