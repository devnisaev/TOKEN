package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.MarketplaceClient;
import com.tokenrealty.gateway.client.PropertyRegistryClient;
import com.tokenrealty.gateway.client.TokenIssuanceClient;
import com.tokenrealty.gateway.dto.BffDtos.ListingDetailResponse;
import com.tokenrealty.gateway.dto.BffDtos.ListingSummary;
import com.tokenrealty.gateway.dto.BffDtos.TokenContractSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BffListingService {

    private final MarketplaceClient marketplaceClient;
    private final PropertyRegistryClient registryClient;
    private final TokenIssuanceClient issuanceClient;

    public ListingDetailResponse getListingDetail(UUID listingId) {
        var listing = marketplaceClient.getListing(listingId);
        var flat = registryClient.getFlat(listing.flatId());
        var contract = issuanceClient.getContractByFlatId(listing.flatId());

        return ListingDetailResponse.builder()
                .listing(toListingSummary(listing))
                .flatId(flat.id())
                .buildingName(flat.buildingName())
                .flatNumber(flat.flatNumber())
                .flatStatus(flat.status())
                .tokenContract(contract != null ? toContractSummary(contract) : null)
                .build();
    }

    private static ListingSummary toListingSummary(MarketplaceClient.ListingView listing) {
        return ListingSummary.builder()
                .listingId(listing.id())
                .listingType(listing.listingType())
                .status(listing.status())
                .priceUsd(listing.priceUsd())
                .tokensAvailable(listing.tokensAvailable())
                .tokensTotal(listing.tokensTotal())
                .minInvestmentTokens(listing.minInvestmentTokens())
                .title(listing.title())
                .build();
    }

    private static TokenContractSummary toContractSummary(TokenIssuanceClient.TokenContractView contract) {
        return TokenContractSummary.builder()
                .contractId(contract.id())
                .contractAddress(contract.contractAddress())
                .tokenSymbol(contract.tokenSymbol())
                .totalSupply(contract.totalSupply())
                .tokenPriceUsd(contract.tokenPriceUsd())
                .status(contract.status())
                .build();
    }
}
