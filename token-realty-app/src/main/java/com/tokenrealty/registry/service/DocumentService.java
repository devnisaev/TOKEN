package com.tokenrealty.registry.service;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.PropertyDocument;
import com.tokenrealty.registry.kafka.command.DocumentUploadedCommand;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import com.tokenrealty.registry.mapper.PropertyMapper;
import com.tokenrealty.registry.repository.BuildingRepository;
import com.tokenrealty.registry.repository.FlatRepository;
import com.tokenrealty.registry.repository.PropertyDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DocumentService {

    private final PropertyDocumentRepository documentRepository;
    private final BuildingRepository buildingRepository;
    private final FlatRepository flatRepository;
    private final PropertyMapper mapper;

    public List<DocumentResponse> findByBuilding(UUID buildingId) {
        if (!buildingRepository.existsById(buildingId)) {
            throw new ResourceNotFoundException("Building", buildingId);
        }
        return mapper.toDocumentResponses(documentRepository.findByBuildingId(buildingId));
    }

    public List<DocumentResponse> findByFlat(UUID flatId) {
        if (!flatRepository.existsById(flatId)) {
            throw new ResourceNotFoundException("Flat", flatId);
        }
        return mapper.toDocumentResponses(documentRepository.findByFlatId(flatId));
    }

    public DocumentResponse findById(UUID id) {
        return mapper.toDocumentResponse(getOrThrow(id));
    }

    @Transactional
    public DocumentResponse registerForBuilding(UUID buildingId, RegisterDocumentRequest request) {
        var building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building", buildingId));

        validateDocumentSource(request);

        PropertyDocument doc = mapper.toDocument(request);
        doc.setBuilding(building);
        doc.setUploadedBy(currentUsername());

        PropertyDocument saved = documentRepository.save(doc);
        log.info("Registered document id={} type={} for building={}", saved.getId(), saved.getDocumentType(), buildingId);
        return mapper.toDocumentResponse(saved);
    }

    @Transactional
    public DocumentResponse registerForFlat(UUID flatId, RegisterDocumentRequest request) {
        var flat = flatRepository.findById(flatId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat", flatId));

        validateDocumentSource(request);

        PropertyDocument doc = mapper.toDocument(request);
        doc.setFlat(flat);
        doc.setBuilding(flat.getBuilding());
        doc.setUploadedBy(currentUsername());

        PropertyDocument saved = documentRepository.save(doc);
        log.info("Registered document id={} type={} for flat={}", saved.getId(), saved.getDocumentType(), flatId);
        return mapper.toDocumentResponse(saved);
    }

    @Transactional
    public void acknowledgeUpload(DocumentUploadedCommand command) {
        PropertyDocument doc = getOrThrow(command.documentId());
        if (doc.getIpfsCid() != null && !doc.getIpfsCid().equals(command.ipfsCid())) {
            throw new ValidationException(
                    "IPFS CID mismatch for document " + command.documentId());
        }
        if (doc.getIpfsCid() == null) {
            doc.setIpfsCid(command.ipfsCid());
            documentRepository.save(doc);
        }
        log.info("Acknowledged document upload id={} type={} cid={}",
                command.documentId(), command.documentType(), command.ipfsCid());
    }

    @Transactional
    public DocumentResponse verify(UUID id) {
        PropertyDocument doc = getOrThrow(id);
        if (Boolean.TRUE.equals(doc.getIsVerified())) {
            throw new ConflictException("Document " + id + " is already verified");
        }
        doc.setIsVerified(true);
        doc.setVerifiedBy(currentUsername());
        log.info("Document {} verified by {}", id, doc.getVerifiedBy());
        return mapper.toDocumentResponse(documentRepository.save(doc));
    }

    @Transactional
    public void delete(UUID id) {
        PropertyDocument doc = getOrThrow(id);
        if (Boolean.TRUE.equals(doc.getIsVerified())) {
            throw new ConflictException("Cannot delete a verified document");
        }
        documentRepository.delete(doc);
        log.info("Deleted document id={}", id);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private PropertyDocument getOrThrow(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    private void validateDocumentSource(RegisterDocumentRequest request) {
        if (request.ipfsCid() == null && request.storageUrl() == null) {
            throw new ConflictException("Either ipfsCid or storageUrl must be provided");
        }
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "system";
    }
}