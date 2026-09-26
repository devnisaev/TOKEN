package com.tokenrealty.search.service;

import com.tokenrealty.search.entity.BuildingIndex;
import com.tokenrealty.search.entity.ListingIndex;
import com.tokenrealty.search.kafka.command.BuildingApprovedCommand;
import com.tokenrealty.search.kafka.command.FlatTokenizedCommand;
import com.tokenrealty.search.kafka.command.ListingCreatedCommand;
import com.tokenrealty.search.kafka.command.ValuationUpdatedCommand;
import com.tokenrealty.search.repository.BuildingIndexRepository;
import com.tokenrealty.search.repository.ListingIndexRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchIndexService {

    private final ListingIndexRepository listingIndexRepository;
    private final BuildingIndexRepository buildingIndexRepository;
    private final BuildingEnrichmentService buildingEnrichmentService;
    private final OptionalSearchIndexSync optionalSearchIndexSync;
    private final Clock clock;

    @Transactional
    public void onListingCreated(ListingCreatedCommand command) {
        Instant indexedAt = clock.instant();
        ListingIndex index = listingIndexRepository.findByListingId(command.listingId())
                .orElseGet(() -> ListingIndex.builder().listingId(command.listingId()).build());

        index.setFlatId(command.flatId());
        index.setListingType(command.listingType());
        index.setPriceUsd(command.priceUsd());
        index.setTokensAvailable(command.tokensAvailable());
        index.setSearchText(buildListingSearchText(index));
        index.setSourceEventId(command.eventId());
        index.setIndexedAt(indexedAt);
        listingIndexRepository.save(index);
        optionalSearchIndexSync.syncListing(index);
    }

    @Transactional
    public void onFlatTokenized(FlatTokenizedCommand command) {
        Instant indexedAt = clock.instant();
        enrichListingsFromFlatTokenized(command, indexedAt);
        upsertBuildingFromFlatTokenized(command, indexedAt);
    }

    @Transactional
    public void onBuildingApproved(BuildingApprovedCommand command) {
        Instant indexedAt = clock.instant();
        BuildingIndex index = buildingIndexRepository.findByBuildingId(command.buildingId())
                .orElseGet(() -> BuildingIndex.builder()
                        .buildingId(command.buildingId())
                        .flatCount(0)
                        .build());

        index.setApprovedAt(command.approvedAt());
        buildingEnrichmentService.enrichBuilding(index, command.buildingId());
        index.setSearchText(buildBuildingSearchText(index));
        index.setSourceEventId(command.eventId());
        index.setIndexedAt(indexedAt);
        buildingIndexRepository.save(index);
        optionalSearchIndexSync.syncBuilding(index);
    }

    @Transactional
    public void onValuationUpdated(ValuationUpdatedCommand command) {
        Instant indexedAt = clock.instant();
        List<ListingIndex> listings = listingIndexRepository.findByFlatId(command.flatId());
        buildingEnrichmentService.enrichListings(listings, command.buildingId());
        for (ListingIndex listing : listings) {
            listing.setBuildingId(command.buildingId());
            listing.setNavPerTokenUsd(command.navPerTokenUsd());
            listing.setSearchText(buildListingSearchText(listing));
            listing.setIndexedAt(indexedAt);
        }
        listingIndexRepository.saveAll(listings);
        listings.forEach(optionalSearchIndexSync::syncListing);

        BuildingIndex building = buildingIndexRepository.findByBuildingId(command.buildingId())
                .orElseGet(() -> BuildingIndex.builder()
                        .buildingId(command.buildingId())
                        .flatCount(0)
                        .build());
        building.setLatestNavPerTokenUsd(command.navPerTokenUsd());
        buildingEnrichmentService.enrichBuilding(building, command.buildingId());
        building.setSearchText(buildBuildingSearchText(building));
        building.setSourceEventId(command.eventId());
        building.setIndexedAt(indexedAt);
        buildingIndexRepository.save(building);
        optionalSearchIndexSync.syncBuilding(building);
    }

    private void enrichListingsFromFlatTokenized(FlatTokenizedCommand command, Instant indexedAt) {
        List<ListingIndex> listings = listingIndexRepository.findByFlatId(command.flatId());
        buildingEnrichmentService.enrichListings(listings, command.buildingId());
        for (ListingIndex listing : listings) {
            listing.setBuildingId(command.buildingId());
            if (listing.getPriceUsd() == null) {
                listing.setPriceUsd(command.tokenPriceUsd());
            }
            listing.setSearchText(buildListingSearchText(listing));
            listing.setIndexedAt(indexedAt);
        }
        listingIndexRepository.saveAll(listings);
        listings.forEach(optionalSearchIndexSync::syncListing);
    }

    private void upsertBuildingFromFlatTokenized(FlatTokenizedCommand command, Instant indexedAt) {
        BuildingIndex building = buildingIndexRepository.findByBuildingId(command.buildingId())
                .orElseGet(() -> BuildingIndex.builder()
                        .buildingId(command.buildingId())
                        .flatCount(0)
                        .build());

        building.setFlatCount(building.getFlatCount() + 1);
        building.setLatestTokenPriceUsd(command.tokenPriceUsd());
        buildingEnrichmentService.enrichBuilding(building, command.buildingId());
        building.setSearchText(buildBuildingSearchText(building));
        building.setSourceEventId(command.eventId());
        building.setIndexedAt(indexedAt);
        buildingIndexRepository.save(building);
        optionalSearchIndexSync.syncBuilding(building);
    }

    static String buildListingSearchText(ListingIndex index) {
        return join(
                index.getListingId(),
                index.getFlatId(),
                index.getBuildingId(),
                index.getBuildingName(),
                index.getCity(),
                index.getListingType(),
                index.getPriceUsd(),
                index.getNavPerTokenUsd());
    }

    static String buildBuildingSearchText(BuildingIndex index) {
        return join(
                index.getBuildingId(),
                index.getBuildingName(),
                index.getCity(),
                index.getApprovedAt(),
                index.getFlatCount(),
                index.getLatestTokenPriceUsd(),
                index.getLatestNavPerTokenUsd());
    }

    private static String join(Object... parts) {
        StringBuilder builder = new StringBuilder();
        for (Object part : parts) {
            if (part == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part);
        }
        return builder.toString();
    }
}
