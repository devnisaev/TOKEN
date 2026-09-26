package com.tokenrealty.document.client;

import com.tokenrealty.document.dto.DocumentDtos.DocumentResponse;
import com.tokenrealty.document.dto.DocumentDtos.RegisterDocumentRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class PropertyRegistryClient {

    private final RestClient restClient;

    public PropertyRegistryClient(@Qualifier("propertyRegistryRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public DocumentResponse registerForBuilding(UUID buildingId, RegisterDocumentRequest request) {
        return restClient.post()
                .uri("/v1/buildings/{buildingId}/documents", buildingId)
                .body(request)
                .retrieve()
                .body(DocumentResponse.class);
    }

    public DocumentResponse registerForFlat(UUID flatId, RegisterDocumentRequest request) {
        return restClient.post()
                .uri("/v1/flats/{flatId}/documents", flatId)
                .body(request)
                .retrieve()
                .body(DocumentResponse.class);
    }

    public DocumentResponse getDocument(UUID documentId) {
        return restClient.get()
                .uri("/v1/documents/{id}", documentId)
                .retrieve()
                .body(DocumentResponse.class);
    }
}
