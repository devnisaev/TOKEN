package com.tokenrealty.valuation.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "revaluation_schedules")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RevaluationSchedule extends BaseEntity {

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "flat_id")
    private UUID flatId;

    @Column(name = "interval_months", nullable = false)
    private int intervalMonths;

    @Column(name = "next_due_at", nullable = false)
    private Instant nextDueAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
