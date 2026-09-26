package com.tokenrealty.reporting.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stuck_saga_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StuckSagaRecord extends BaseEntity {

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "saga_id", nullable = false)
    private UUID sagaId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "current_step", nullable = false)
    private String currentStep;

    @Column(name = "stuck_at", nullable = false)
    private Instant stuckAt;
}
