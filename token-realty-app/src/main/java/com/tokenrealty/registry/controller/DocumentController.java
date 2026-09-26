package com.tokenrealty.registry.controller;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Property document store")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/buildings/{buildingId}/documents")
    @Operation(summary = "List all documents attached to a building")
    public List<DocumentResponse> listByBuilding(@PathVariable UUID buildingId) {
        return documentService.findByBuilding(buildingId);
    }

    @GetMapping("/flats/{flatId}/documents")
    @Operation(summary = "List all documents attached to a flat")
    public List<DocumentResponse> listByFlat(@PathVariable UUID flatId) {
        return documentService.findByFlat(flatId);
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "Get a single document by id")
    public DocumentResponse getById(@PathVariable UUID id) {
        return documentService.findById(id);
    }

    @PostMapping("/buildings/{buildingId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    @Operation(summary = "Register a document against a building")
    public DocumentResponse addToBuilding(
            @PathVariable UUID buildingId,
            @Valid @RequestBody RegisterDocumentRequest request) {
        return documentService.registerForBuilding(buildingId, request);
    }

    @PostMapping("/flats/{flatId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    @Operation(summary = "Register a document against a flat")
    public DocumentResponse addToFlat(
            @PathVariable UUID flatId,
            @Valid @RequestBody RegisterDocumentRequest request) {
        return documentService.registerForFlat(flatId, request);
    }

    @PatchMapping("/documents/{id}/verify")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE') or hasRole('SERVICE')")
    @Operation(summary = "Mark a document as verified by compliance")
    public DocumentResponse verify(@PathVariable UUID id) {
        return documentService.verify(id);
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an unverified document")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
