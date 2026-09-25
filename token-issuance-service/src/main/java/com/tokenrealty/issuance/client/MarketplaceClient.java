package com.tokenrealty.issuance.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MarketplaceClient {

    private final RestClient restClient;

    public MarketplaceClient(@Qualifier("marketplaceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public TradeSnapshot getTradeByOrderId(UUID orderId) {
        return restClient.get()
                .uri("/v1/orders/{orderId}/trade", orderId)
                .retrieve()
                .body(TradeSnapshot.class);
    }

    public record TradeSnapshot(
            UUID id,
            UUID orderId,
            UUID flatId,
            UUID contractId,
            UUID buyerId,
            String buyerWallet,
            long tokenAmount,
            BigDecimal totalPriceUsd,
            String status,
            UUID paymentId,
            UUID transferId
    ) {
    }
}
