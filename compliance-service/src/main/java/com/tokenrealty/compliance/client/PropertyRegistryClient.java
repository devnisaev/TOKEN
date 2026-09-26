package com.tokenrealty.compliance.client;

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

    public RegistryDocumentResponse verifyDocument(UUID documentId) {
        return restClient.patch()
                .uri("/v1/documents/{id}/verify", documentId)
                .retrieve()
                .body(RegistryDocumentResponse.class);
    }
}
