package com.tokenrealty.compliance.client;

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

    public RegistryDocumentResponse verifyDocument(UUID documentId) {
        return patch(
                "/v1/documents/{id}/verify",
                RegistryDocumentResponse.class,
                DownstreamServices.PROPERTY_REGISTRY,
                documentId);
    }
}
