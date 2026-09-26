package com.tokenrealty.integration.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DocumentClient extends DownstreamRestClientSupport {

    public DocumentClient(@Qualifier("documentRestClient") RestClient restClient) {
        super(restClient);
    }

    public void forwardStorageWebhook(
            String provider,
            String rawBody,
            String payloadDigest,
            String signature) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (payloadDigest != null) {
            headers.put("X-Payload-Digest", payloadDigest);
        }
        if (signature != null) {
            headers.put("X-Signature", signature);
        }
        headers.put("Content-Type", "application/json");
        postVoid(
                "/v1/documents/webhooks/{provider}",
                rawBody,
                DownstreamServices.DOCUMENT,
                headers,
                provider);
    }
}
