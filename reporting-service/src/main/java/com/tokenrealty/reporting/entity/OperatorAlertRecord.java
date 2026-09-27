package com.tokenrealty.reporting.entity;

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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operator_alert_records", indexes = {
        @Index(name = "idx_operator_alert_dedupe", columnList = "dedupe_key", unique = true),
        @Index(name = "idx_operator_alert_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorAlertRecord extends BaseEntity {

    @Column(name = "dedupe_key", nullable = false, length = 128)
    private String dedupeKey;

    @Column(name = "alert_type", nullable = false, length = 32)
    private String alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private AlertSeverity severity = AlertSeverity.WARNING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "building_id")
    private UUID buildingId;

    @Column(name = "flat_id")
    private UUID flatId;

    @Column(nullable = false, length = 512)
    private String message;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    public enum AlertSeverity {
        WARNING, CRITICAL
    }

    public enum AlertStatus {
        OPEN, ACKNOWLEDGED
    }
}
