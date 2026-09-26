package com.tokenrealty.issuance.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class RentalClient extends DownstreamRestClientSupport {

    public RentalClient(@Qualifier("rentalRestClient") RestClient restClient) {
        super(restClient);
    }

    public BigDecimal getRentCollectedForPeriod(UUID flatId, String period) {
        RentSummaryResponse response = get(
                uriBuilder -> uriBuilder
                        .path("/v1/rent-payments/summary")
                        .queryParam("flatId", flatId)
                        .queryParam("period", period)
                        .build(),
                RentSummaryResponse.class,
                DownstreamServices.RENTAL,
                null);
        return response != null ? response.totalAmount() : BigDecimal.ZERO;
    }

    record RentSummaryResponse(UUID flatId, String period, BigDecimal totalAmount) {
    }
}
