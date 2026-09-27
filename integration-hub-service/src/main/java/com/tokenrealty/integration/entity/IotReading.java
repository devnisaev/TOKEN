package com.tokenrealty.integration.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iot_readings", indexes = {
        @Index(name = "idx_iot_readings_flat", columnList = "flat_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IotReading extends BaseEntity {

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "provider", nullable = false, length = 64)
    private String provider;

    @Column(name = "occupancy_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal occupancyPct;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
