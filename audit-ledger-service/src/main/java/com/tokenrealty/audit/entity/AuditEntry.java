package com.tokenrealty.audit.entity;

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
import java.util.UUID;

@Entity
@Table(name = "audit_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEntry extends BaseEntity {

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private AuditSubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
