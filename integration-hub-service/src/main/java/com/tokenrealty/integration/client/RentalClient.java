package com.tokenrealty.integration.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RentalClient extends DownstreamRestClientSupport {

    public RentalClient(@Qualifier("rentalRestClient") RestClient restClient) {
        super(restClient);
    }

    public void forwardOperatorRevenueWebhook(String provider, String rawBody) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        postVoid(
                "/v1/operator-revenue/feeds/{provider}",
                rawBody,
                DownstreamServices.RENTAL,
                headers,
                provider);
    }
}
