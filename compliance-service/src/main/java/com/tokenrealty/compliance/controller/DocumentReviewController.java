package com.tokenrealty.compliance.controller;

import com.tokenrealty.compliance.dto.ComplianceDtos.DocumentReviewResponse;
import com.tokenrealty.compliance.service.DocumentReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/compliance/document-reviews")
@RequiredArgsConstructor
@Tag(name = "Document Reviews", description = "Data room document compliance review queue")
public class DocumentReviewController {

    private final DocumentReviewService documentReviewService;

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE')")
    @Operation(summary = "List documents awaiting compliance review")
    public List<DocumentReviewResponse> listPending() {
        return documentReviewService.findPending();
    }

    @PatchMapping("/{documentId}/verify")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE')")
    @Operation(summary = "Verify a document in Property Registry after review")
    public DocumentReviewResponse verify(@PathVariable UUID documentId) {
        return documentReviewService.verify(documentId);
    }
}
