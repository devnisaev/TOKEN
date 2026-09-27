package com.tokenrealty.reporting.entity;

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

@Entity
@Table(name = "operator_kpi_snapshot_records", indexes = {
        @Index(name = "idx_kpi_snapshot_at", columnList = "snapshot_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorKpiSnapshotRecord extends BaseEntity {

    @Column(name = "average_occupancy_pct", precision = 5, scale = 2)
    private BigDecimal averageOccupancyPct;

    @Column(name = "average_carbon_score", precision = 5, scale = 2)
    private BigDecimal averageCarbonScore;

    @Column(name = "average_health_score", precision = 5, scale = 2)
    private BigDecimal averageHealthScore;

    @Column(name = "tracked_asset_count", nullable = false)
    private long trackedAssetCount;

    @Column(name = "at_risk_asset_count", nullable = false)
    private long atRiskAssetCount;

    @Column(name = "open_operator_alert_count", nullable = false)
    private long openOperatorAlertCount;

    @Column(name = "open_maintenance_ticket_count", nullable = false)
    private long openMaintenanceTicketCount;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;
}
