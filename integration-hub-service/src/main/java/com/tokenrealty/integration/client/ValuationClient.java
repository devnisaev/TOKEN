package com.tokenrealty.integration.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ValuationClient extends DownstreamRestClientSupport {

    public ValuationClient(@Qualifier("valuationRestClient") RestClient restClient) {
        super(restClient);
    }

    public void forwardValuationFeedWebhook(String provider, String rawBody) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        postVoid(
                "/v1/valuations/feeds/{provider}",
                rawBody,
                DownstreamServices.VALUATION,
                headers,
                provider);
    }
}
