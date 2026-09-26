package com.tokenrealty.registry.service;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.entity.Flat;
import com.tokenrealty.registry.entity.PropertyDocument;
import com.tokenrealty.registry.kafka.command.DocumentUploadedCommand;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import com.tokenrealty.registry.mapper.PropertyMapper;
import com.tokenrealty.registry.repository.BuildingRepository;
import com.tokenrealty.registry.repository.FlatRepository;
import com.tokenrealty.registry.repository.PropertyDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService unit tests")
class DocumentServiceTest {

    @Mock PropertyDocumentRepository documentRepository;
    @Mock BuildingRepository buildingRepository;
    @Mock FlatRepository flatRepository;
    @Mock PropertyMapper mapper;
    @InjectMocks DocumentService documentService;

    private UUID buildingId;
    private UUID flatId;
    private UUID docId;
    private Building building;
    private Flat flat;
    private PropertyDocument document;
    private DocumentResponse documentResponse;

    @BeforeEach
    void setUp() {
        buildingId = UUID.randomUUID();
        flatId = UUID.randomUUID();
        docId = UUID.randomUUID();

        building = Building.builder()
                .name("Sunrise Tower")
                .address("123 Main St")
                .city("Bishkek")
                .country("KG")
                .build();

        flat = Flat.builder()
                .flatNumber("101")
                .building(building)
                .status(Flat.FlatStatus.AVAILABLE)
                .build();

        document = PropertyDocument.builder()
                .documentName("Title Deed")
                .documentType(PropertyDocument.DocumentType.TITLE_DEED)
                .ipfsCid("Qm123456789")
                .isVerified(false)
                .build();

        documentResponse = DocumentResponse.builder()
                .id(docId)
                .documentName("Title Deed")
                .documentType(PropertyDocument.DocumentType.TITLE_DEED)
                .ipfsCid("Qm123456789")
                .isVerified(false)
                .build();

        // Set up security context so currentUsername() works
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, List.of()));
    }

    @Test
    @DisplayName("registerForBuilding saves document linked to building")
    void registerForBuilding_savesDocument() {
        var request = new RegisterDocumentRequest(
                "Title Deed", PropertyDocument.DocumentType.TITLE_DEED,
                "Qm123456789", null, 102400L, "application/pdf");

        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
        when(mapper.toDocument(request)).thenReturn(document);
        when(documentRepository.save(document)).thenReturn(document);
        when(mapper.toDocumentResponse(document)).thenReturn(documentResponse);

        var result = documentService.registerForBuilding(buildingId, request);

        assertThat(result.documentName()).isEqualTo("Title Deed");
        assertThat(document.getBuilding()).isEqualTo(building);
        assertThat(document.getUploadedBy()).isEqualTo("testuser");
        verify(documentRepository).save(document);
    }

    @Test
    @DisplayName("registerForFlat links document to both flat and its building")
    void registerForFlat_linksDocumentToFlatAndBuilding() {
        var request = new RegisterDocumentRequest(
                "Floor Plan", PropertyDocument.DocumentType.FLOOR_PLAN,
                "Qm987654321", null, 51200L, "image/png");

        when(flatRepository.findById(flatId)).thenReturn(Optional.of(flat));
        when(mapper.toDocument(request)).thenReturn(document);
        when(documentRepository.save(document)).thenReturn(document);
        when(mapper.toDocumentResponse(document)).thenReturn(documentResponse);

        documentService.registerForFlat(flatId, request);

        assertThat(document.getFlat()).isEqualTo(flat);
        assertThat(document.getBuilding()).isEqualTo(building); // inherited from flat
    }

    @Test
    @DisplayName("registerForBuilding throws ConflictException when neither ipfsCid nor storageUrl provided")
    void register_throwsConflict_whenNoSource() {
        var request = new RegisterDocumentRequest(
                "Mystery Doc", PropertyDocument.DocumentType.OTHER,
                null, null, null, null);

        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
        // No mapper stub needed — validateDocumentSource() throws before mapper.toDocument() is called
        document.setIpfsCid(null);
        document.setStorageUrl(null);

        assertThatThrownBy(() -> documentService.registerForBuilding(buildingId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ipfsCid or storageUrl");
    }

    @Test
    @DisplayName("acknowledgeUpload is no-op when document CID already matches")
    void acknowledgeUpload_noOpWhenCidMatches() {
        document.setId(docId);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        documentService.acknowledgeUpload(new DocumentUploadedCommand(
                docId, buildingId, null, "TITLE_DEED", "Qm123456789", null, Instant.now()));

        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("acknowledgeUpload backfills missing CID from event")
    void acknowledgeUpload_backfillsMissingCid() {
        document.setId(docId);
        document.setIpfsCid(null);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentRepository.save(document)).thenReturn(document);

        documentService.acknowledgeUpload(new DocumentUploadedCommand(
                docId, buildingId, null, "TITLE_DEED", "Qm123456789", null, Instant.now()));

        assertThat(document.getIpfsCid()).isEqualTo("Qm123456789");
        verify(documentRepository).save(document);
    }

    @Test
    @DisplayName("acknowledgeUpload throws ValidationException on CID mismatch")
    void acknowledgeUpload_throwsOnCidMismatch() {
        document.setId(docId);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.acknowledgeUpload(new DocumentUploadedCommand(
                docId, buildingId, null, "TITLE_DEED", "QmDifferent", null, Instant.now())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CID mismatch");
    }

    @Test
    @DisplayName("verify sets isVerified=true and records verifier")
    void verify_setsVerifiedTrue() {
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(documentRepository.save(document)).thenReturn(document);
        when(mapper.toDocumentResponse(document)).thenReturn(
                new DocumentResponse(
                        documentResponse.id(), documentResponse.documentName(), documentResponse.documentType(),
                        documentResponse.ipfsCid(), documentResponse.storageUrl(), documentResponse.fileSizeBytes(),
                        documentResponse.mimeType(), true, "testuser",
                        documentResponse.uploadedBy(), documentResponse.createdAt()));

        var result = documentService.verify(docId);

        assertThat(document.getIsVerified()).isTrue();
        assertThat(document.getVerifiedBy()).isEqualTo("testuser");
        assertThat(result.isVerified()).isTrue();
    }

    @Test
    @DisplayName("verify throws ConflictException when already verified")
    void verify_throwsConflict_whenAlreadyVerified() {
        document.setIsVerified(true);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.verify(docId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already verified");
    }

    @Test
    @DisplayName("delete throws ConflictException when document is verified")
    void delete_throwsConflict_whenVerified() {
        document.setIsVerified(true);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.delete(docId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("verified document");

        verify(documentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete removes unverified document")
    void delete_removesUnverifiedDocument() {
        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        documentService.delete(docId);

        verify(documentRepository).delete(document);
    }

    @Test
    @DisplayName("findByBuilding throws ResourceNotFoundException for unknown building")
    void findByBuilding_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(buildingRepository.existsById(unknownId)).thenReturn(false);

        assertThatThrownBy(() -> documentService.findByBuilding(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}