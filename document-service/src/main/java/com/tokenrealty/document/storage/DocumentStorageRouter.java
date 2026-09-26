package com.tokenrealty.document.storage;

import com.tokenrealty.document.dto.DocumentDtos.DocumentType;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DocumentStorageRouter {

    private static final Set<DocumentType> PRIVATE_TYPES = Set.of(
            DocumentType.KYC_DOCUMENT,
            DocumentType.INSURANCE_POLICY);

    private final IpfsStorageService ipfsStorageService;
    private final MinioStorageService minioStorageService;

    public DocumentStorageRouter(IpfsStorageService ipfsStorageService, MinioStorageService minioStorageService) {
        this.ipfsStorageService = ipfsStorageService;
        this.minioStorageService = minioStorageService;
    }

    public StorageResult store(byte[] content, String filename, DocumentType documentType) {
        if (PRIVATE_TYPES.contains(documentType)) {
            return new StorageResult(null, minioStorageService.store(content, filename));
        }
        return new StorageResult(ipfsStorageService.pin(content, filename), null);
    }

    public record StorageResult(String ipfsCid, String storageUrl) {}
}
