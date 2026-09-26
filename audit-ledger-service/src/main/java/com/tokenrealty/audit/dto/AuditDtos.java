package com.tokenrealty.audit.dto;

import com.tokenrealty.audit.entity.AuditSubjectType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditEntryView(
            UUID id,
            String eventType,
            AuditSubjectType subjectType,
            UUID subjectId,
            UUID actorId,
            String summary,
            Instant occurredAt
    ) {
    }

    public record AuditExportResponse(
            List<AuditEntryView> entries,
            Instant generatedAt
    ) {
    }
}
