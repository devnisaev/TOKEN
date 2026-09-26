package com.tokenrealty.issuance.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class RentalClient {

    private final RestClient restClient;

    public RentalClient(@Qualifier("rentalRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public BigDecimal getRentCollectedForPeriod(UUID flatId, String period) {
        RentSummaryResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/rent-payments/summary")
                        .queryParam("flatId", flatId)
                        .queryParam("period", period)
                        .build())
                .retrieve()
                .body(RentSummaryResponse.class);
        return response != null ? response.totalAmount() : BigDecimal.ZERO;
    }

    record RentSummaryResponse(UUID flatId, String period, BigDecimal totalAmount) {
    }
}
