package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.MarketplaceClient;
import com.tokenrealty.gateway.client.PropertyRegistryClient;
import com.tokenrealty.gateway.client.TokenIssuanceClient;
import com.tokenrealty.gateway.dto.BffDtos.FlatDetailResponse;
import com.tokenrealty.gateway.dto.BffDtos.ListingSummary;
import com.tokenrealty.gateway.dto.BffDtos.TokenContractSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BffFlatService {

    private final PropertyRegistryClient registryClient;
    private final TokenIssuanceClient issuanceClient;
    private final MarketplaceClient marketplaceClient;

    public FlatDetailResponse getFlatDetail(UUID flatId) {
        var flat = registryClient.getFlat(flatId);
        var contract = issuanceClient.getContractByFlatId(flatId);
        var listing = marketplaceClient.findActiveListingByFlatId(flatId);

        return FlatDetailResponse.builder()
                .flatId(flat.id())
                .buildingId(flat.buildingId())
                .buildingName(flat.buildingName())
                .flatNumber(flat.flatNumber())
                .floor(flat.floor())
                .areaSqm(flat.areaSqm())
                .status(flat.status())
                .tokenContract(contract != null ? toContractSummary(contract) : registryContract(flat))
                .listing(listing != null ? toListingSummary(listing) : null)
                .build();
    }

    private static TokenContractSummary registryContract(PropertyRegistryClient.FlatView flat) {
        if (flat.tokenContractAddress() == null) {
            return null;
        }
        return TokenContractSummary.builder()
                .contractAddress(flat.tokenContractAddress())
                .totalSupply(flat.totalTokens())
                .tokenPriceUsd(flat.tokenPriceUsd())
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
}
