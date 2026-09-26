package com.tokenrealty.issuance.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MarketplaceClient extends DownstreamRestClientSupport {

    public MarketplaceClient(@Qualifier("marketplaceRestClient") RestClient restClient) {
        super(restClient);
    }

    public TradeSnapshot getTradeByOrderId(UUID orderId) {
        return get(
                "/v1/orders/{orderId}/trade",
                TradeSnapshot.class,
                DownstreamServices.MARKETPLACE,
                orderId);
    }

    public record TradeSnapshot(
            UUID id,
            UUID orderId,
            UUID flatId,
            UUID contractId,
            UUID buyerId,
            String buyerWallet,
            String sellerWallet,
            String listingType,
            long tokenAmount,
            BigDecimal totalPriceUsd,
            String status,
            UUID paymentId,
            UUID transferId
    ) {
    }
}
