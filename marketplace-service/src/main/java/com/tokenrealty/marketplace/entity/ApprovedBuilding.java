package com.tokenrealty.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approved_buildings")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApprovedBuilding {

    @Id
    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;

    @Column(name = "approved_by", nullable = false, length = 128)
    private String approvedBy;
}
