package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.MarketplaceClient;
import com.tokenrealty.gateway.client.PropertyRegistryClient;
import com.tokenrealty.gateway.client.TokenIssuanceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BffFlatService unit tests")
class BffFlatServiceTest {

    @Mock PropertyRegistryClient registryClient;
    @Mock TokenIssuanceClient issuanceClient;
    @Mock MarketplaceClient marketplaceClient;
    @InjectMocks BffFlatService flatService;

    @Test
    @DisplayName("getFlatDetail aggregates flat, contract, and listing")
    void getFlatDetail_aggregates() {
        UUID flatId = UUID.randomUUID();
        UUID buildingId = UUID.randomUUID();

        when(registryClient.getFlat(flatId)).thenReturn(new PropertyRegistryClient.FlatView(
                flatId, buildingId, "Sunrise Tower", "101", 1, 85.0,
                "TOKENIZED", "0xContract", 1000L, BigDecimal.valueOf(45)));
        when(issuanceClient.getContractByFlatId(flatId)).thenReturn(new TokenIssuanceClient.TokenContractView(
                UUID.randomUUID(), flatId, "0xContract", "SUN-101", 1000L, BigDecimal.valueOf(45), "ACTIVE"));
        when(marketplaceClient.findActiveListingByFlatId(flatId)).thenReturn(new MarketplaceClient.ListingView(
                UUID.randomUUID(), flatId, UUID.randomUUID(), "PRIMARY", "ACTIVE",
                BigDecimal.valueOf(45), 800, 1000, 10, "Sunrise Tower Flat 101"));

        var detail = flatService.getFlatDetail(flatId);

        assertThat(detail.flatId()).isEqualTo(flatId);
        assertThat(detail.buildingName()).isEqualTo("Sunrise Tower");
        assertThat(detail.tokenContract()).isNotNull();
        assertThat(detail.listing()).isNotNull();
        assertThat(detail.listing().tokensAvailable()).isEqualTo(800);
    }
}
