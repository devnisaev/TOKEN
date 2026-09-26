package com.tokenrealty.audit.controller;

import com.tokenrealty.audit.dto.AuditDtos.AuditEntryView;
import com.tokenrealty.audit.dto.AuditDtos.AuditExportResponse;
import com.tokenrealty.audit.entity.AuditEntry;
import com.tokenrealty.audit.service.AuditLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLedgerService auditLedgerService;
    private final Clock clock;

    @GetMapping("/investor/{investorId}")
    public Page<AuditEntryView> byInvestor(
            @PathVariable UUID investorId,
            @PageableDefault(size = 20) Pageable pageable) {
        return auditLedgerService.findByInvestor(investorId, pageable).map(this::toView);
    }

    @GetMapping("/flat/{flatId}")
    public Page<AuditEntryView> byFlat(
            @PathVariable UUID flatId,
            @PageableDefault(size = 20) Pageable pageable) {
        return auditLedgerService.findByFlat(flatId, pageable).map(this::toView);
    }

    @GetMapping("/export")
    public AuditExportResponse export() {
        List<AuditEntryView> items = auditLedgerService.exportAll().stream().map(this::toView).toList();
        return new AuditExportResponse(items, clock.instant());
    }

    private AuditEntryView toView(AuditEntry entry) {
        return new AuditEntryView(
                entry.getId(),
                entry.getEventType(),
                entry.getSubjectType(),
                entry.getSubjectId(),
                entry.getActorId(),
                entry.getSummary(),
                entry.getOccurredAt()
        );
    }
}
