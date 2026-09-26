package com.tokenrealty.document.service;

import com.tokenrealty.document.client.PropertyRegistryClient;
import com.tokenrealty.document.dto.DocumentDtos.DocumentResponse;
import com.tokenrealty.document.dto.DocumentDtos.DocumentType;
import com.tokenrealty.document.dto.DocumentDtos.RegisterDocumentRequest;
import com.tokenrealty.document.kafka.port.DocumentUploadedPublisher;
import com.tokenrealty.document.storage.DocumentStorageRouter;
import com.tokenrealty.document.storage.DocumentStorageRouter.StorageResult;
import com.tokenrealty.web.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentUploadService unit tests")
class DocumentUploadServiceTest {

    @Mock DocumentStorageRouter storageRouter;
    @Mock PropertyRegistryClient registryClient;
    @Mock DocumentUploadedPublisher documentUploadedPublisher;
    @InjectMocks DocumentUploadService documentUploadService;

    @Test
    @DisplayName("upload registers flat document and publishes event")
    void uploadFlatDocument() {
        UUID flatId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "deed.pdf", "application/pdf", "pdf-content".getBytes());

        when(storageRouter.store(any(), eq("deed.pdf"), eq(DocumentType.TITLE_DEED)))
                .thenReturn(new StorageResult("bafyTestCid", null));
        when(registryClient.registerForFlat(eq(flatId), any(RegisterDocumentRequest.class)))
                .thenReturn(DocumentResponse.builder()
                        .id(documentId)
                        .documentName("Title deed")
                        .documentType(DocumentType.TITLE_DEED)
                        .ipfsCid("bafyTestCid")
                        .build());

        DocumentResponse response = documentUploadService.upload(
                file, "Title deed", DocumentType.TITLE_DEED, null, flatId);

        assertThat(response.id()).isEqualTo(documentId);
        verify(documentUploadedPublisher).publishDocumentUploaded(any());
    }

    @Test
    @DisplayName("upload rejects missing target")
    void rejectsMissingTarget() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "deed.pdf", "application/pdf", "pdf-content".getBytes());

        assertThatThrownBy(() -> documentUploadService.upload(
                file, "Title deed", DocumentType.TITLE_DEED, null, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("upload registers building document via IPFS")
    void uploadBuildingDocument() {
        UUID buildingId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "plan.pdf", "application/pdf", "plan".getBytes());

        when(storageRouter.store(any(), eq("plan.pdf"), eq(DocumentType.FLOOR_PLAN)))
                .thenReturn(new StorageResult("bafyBuilding", null));
        when(registryClient.registerForBuilding(eq(buildingId), any(RegisterDocumentRequest.class)))
                .thenReturn(DocumentResponse.builder()
                        .id(UUID.randomUUID())
                        .documentName("Floor plan")
                        .documentType(DocumentType.FLOOR_PLAN)
                        .ipfsCid("bafyBuilding")
                        .build());

        documentUploadService.upload(file, "Floor plan", DocumentType.FLOOR_PLAN, buildingId, null);

        ArgumentCaptor<RegisterDocumentRequest> captor = ArgumentCaptor.forClass(RegisterDocumentRequest.class);
        verify(registryClient).registerForBuilding(eq(buildingId), captor.capture());
        assertThat(captor.getValue().ipfsCid()).isEqualTo("bafyBuilding");
        assertThat(captor.getValue().storageUrl()).isNull();
    }

    @Test
    @DisplayName("KYC document routes to private MinIO storageUrl")
    void uploadKycDocumentUsesMinio() {
        UUID buildingId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "passport.pdf", "application/pdf", "kyc".getBytes());

        when(storageRouter.store(any(), eq("passport.pdf"), eq(DocumentType.KYC_DOCUMENT)))
                .thenReturn(new StorageResult(null, "s3://tokenrealty-private/kyc/passport.pdf"));
        when(registryClient.registerForBuilding(eq(buildingId), any(RegisterDocumentRequest.class)))
                .thenReturn(DocumentResponse.builder()
                        .id(UUID.randomUUID())
                        .documentName("Passport")
                        .documentType(DocumentType.KYC_DOCUMENT)
                        .storageUrl("s3://tokenrealty-private/kyc/passport.pdf")
                        .build());

        documentUploadService.upload(file, "Passport", DocumentType.KYC_DOCUMENT, buildingId, null);

        ArgumentCaptor<RegisterDocumentRequest> captor = ArgumentCaptor.forClass(RegisterDocumentRequest.class);
        verify(registryClient).registerForBuilding(eq(buildingId), captor.capture());
        assertThat(captor.getValue().ipfsCid()).isNull();
        assertThat(captor.getValue().storageUrl()).isEqualTo("s3://tokenrealty-private/kyc/passport.pdf");
    }
}
