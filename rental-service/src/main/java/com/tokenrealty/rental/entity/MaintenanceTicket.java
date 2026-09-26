package com.tokenrealty.rental.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "maintenance_tickets", indexes = {
        @Index(name = "idx_maintenance_tickets_lease_id", columnList = "lease_id"),
        @Index(name = "idx_maintenance_tickets_tenant_id", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MaintenanceTicket extends BaseEntity {

    @Column(name = "lease_id", nullable = false)
    private UUID leaseId;

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    public enum TicketStatus {
        OPEN,
        RESOLVED
    }
}
