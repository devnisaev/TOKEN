package com.tokenrealty.document.client;

import com.tokenrealty.document.dto.DocumentDtos.DocumentResponse;
import com.tokenrealty.document.dto.DocumentDtos.RegisterDocumentRequest;
import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class PropertyRegistryClient extends DownstreamRestClientSupport {

    public PropertyRegistryClient(@Qualifier("propertyRegistryRestClient") RestClient restClient) {
        super(restClient);
    }

    public DocumentResponse registerForBuilding(UUID buildingId, RegisterDocumentRequest request) {
        return post(
                "/v1/buildings/{buildingId}/documents",
                request,
                DocumentResponse.class,
                DownstreamServices.PROPERTY_REGISTRY,
                buildingId);
    }

    public DocumentResponse registerForFlat(UUID flatId, RegisterDocumentRequest request) {
        return post(
                "/v1/flats/{flatId}/documents",
                request,
                DocumentResponse.class,
                DownstreamServices.PROPERTY_REGISTRY,
                flatId);
    }

    public DocumentResponse getDocument(UUID documentId) {
        return get(
                "/v1/documents/{id}",
                DocumentResponse.class,
                DownstreamServices.PROPERTY_REGISTRY,
                documentId);
    }
}
