package com.tokenrealty.indexer.controller;

import com.tokenrealty.indexer.entity.IndexedEvent;
import com.tokenrealty.indexer.entity.IndexerCursor;
import com.tokenrealty.indexer.entity.ReconciliationMismatch;
import com.tokenrealty.indexer.repository.IndexedEventRepository;
import com.tokenrealty.indexer.repository.IndexerCursorRepository;
import com.tokenrealty.indexer.service.BalanceReconciliationService;
import com.tokenrealty.indexer.service.BalanceRemediationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/indexer")
@RequiredArgsConstructor
@Tag(name = "Indexer", description = "On-chain event index and reconciliation status")
public class IndexerController {

    private final IndexerCursorRepository cursorRepository;
    private final IndexedEventRepository eventRepository;
    private final BalanceReconciliationService reconciliationService;
    private final BalanceRemediationService remediationService;

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    @Operation(summary = "Indexer cursor status")
    public IndexerStatusResponse status() {
        return cursorRepository.findById(IndexerCursor.DEFAULT_ID)
                .map(c -> new IndexerStatusResponse(c.getId(), c.getLastBlockNumber().longValue()))
                .orElse(new IndexerStatusResponse(IndexerCursor.DEFAULT_ID, 0L));
    }

    @GetMapping("/events")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    @Operation(summary = "Recently indexed on-chain events")
    public Page<IndexedEvent> events(@PageableDefault(size = 20) Pageable pageable) {
        return eventRepository.findAllByOrderByBlockNumberDesc(pageable);
    }

    @GetMapping("/reconciliation")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE')")
    @Operation(summary = "Open on-chain vs DB balance mismatches")
    public List<ReconciliationMismatch> reconciliation() {
        return reconciliationService.openMismatches();
    }

    @PostMapping("/reconciliation/{mismatchId}/remediate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sync holder balance from chain and resolve mismatch")
    public ReconciliationMismatch remediate(@PathVariable UUID mismatchId) {
        return remediationService.remediate(mismatchId);
    }

    public record IndexerStatusResponse(String cursorId, long lastBlockNumber) {
    }
}
