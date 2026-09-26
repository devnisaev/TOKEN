package com.tokenrealty.document.service;

import com.tokenrealty.document.client.PropertyRegistryClient;
import com.tokenrealty.document.dto.DocumentDtos.DocumentResponse;
import com.tokenrealty.document.dto.DocumentDtos.DocumentType;
import com.tokenrealty.document.dto.DocumentDtos.RegisterDocumentRequest;
import com.tokenrealty.document.kafka.port.DocumentUploadedPublisher;
import com.tokenrealty.document.storage.IpfsStorageService;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentUploadService {

    private final IpfsStorageService ipfsStorageService;
    private final PropertyRegistryClient registryClient;
    private final DocumentUploadedPublisher documentUploadedPublisher;

    public DocumentResponse getDocument(UUID documentId) {
        return registryClient.getDocument(documentId);
    }

    @Transactional
    public DocumentResponse upload(
            MultipartFile file,
            String documentName,
            DocumentType documentType,
            UUID buildingId,
            UUID flatId
    ) {
        validateTarget(buildingId, flatId);
        byte[] content = readContent(file);
        String cid = ipfsStorageService.pin(content, file.getOriginalFilename());
        RegisterDocumentRequest request = RegisterDocumentRequest.builder()
                .documentName(documentName)
                .documentType(documentType)
                .ipfsCid(cid)
                .fileSizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .build();

        DocumentResponse response = buildingId != null
                ? registryClient.registerForBuilding(buildingId, request)
                : registryClient.registerForFlat(flatId, request);

        documentUploadedPublisher.publishDocumentUploaded(
                new DocumentUploadedPublisher.DocumentUploadedEvent(
                        response.id(),
                        buildingId,
                        flatId,
                        documentType.name(),
                        cid,
                        Instant.now()));
        return response;
    }

    private static void validateTarget(UUID buildingId, UUID flatId) {
        if (buildingId == null && flatId == null) {
            throw new ValidationException("Either buildingId or flatId is required");
        }
        if (buildingId != null && flatId != null) {
            throw new ValidationException("Provide buildingId or flatId, not both");
        }
    }

    private static byte[] readContent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is required");
        }
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new ValidationException("Failed to read uploaded file");
        }
    }
}
