package com.tokenrealty.document.controller;

import com.tokenrealty.document.dto.DocumentDtos.DocumentResponse;
import com.tokenrealty.document.dto.DocumentDtos.DocumentType;
import com.tokenrealty.document.service.DocumentUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Upload property documents to IPFS and register in Property Registry")
public class DocumentController {

    private final DocumentUploadService documentUploadService;

    @GetMapping("/{documentId}")
    @Operation(summary = "Get document metadata from Property Registry")
    public DocumentResponse getById(@PathVariable UUID documentId) {
        return documentUploadService.getDocument(documentId);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a document to IPFS and register against a building or flat")
    public DocumentResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam String documentName,
            @RequestParam DocumentType documentType,
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) UUID flatId
    ) {
        return documentUploadService.upload(file, documentName, documentType, buildingId, flatId);
    }
}
