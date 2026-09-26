package com.tokenrealty.integration.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "integration_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationDelivery extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false, length = 32)
    private IntegrationType integrationType;

    @Column(nullable = false, length = 64)
    private String provider;

    @Lob
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "payload_digest", length = 256)
    private String payloadDigest;

    @Column(length = 512)
    private String signature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IntegrationDeliveryStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;
}
