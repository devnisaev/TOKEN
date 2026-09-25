package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.exception.ConflictException;
import com.tokenrealty.marketplace.exception.ResourceNotFoundException;
import com.tokenrealty.marketplace.exception.ValidationException;
import com.tokenrealty.marketplace.kafka.MarketplaceEventPublisher;
import com.tokenrealty.marketplace.mapper.MarketplaceMapper;
import com.tokenrealty.marketplace.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListingService {

    private final ListingRepository listingRepository;
    private final MarketplaceMapper mapper;
    private final MarketplaceEventPublisher eventPublisher;

    public Page<ListingResponse> findAll(Listing.ListingStatus status, UUID flatId, Pageable pageable) {
        if (flatId != null) {
            return listingRepository.findByFlatId(flatId, pageable).map(mapper::toListingResponse);
        }
        if (status != null) {
            return listingRepository.findByStatus(status, pageable).map(mapper::toListingResponse);
        }
        return listingRepository.findAll(pageable).map(mapper::toListingResponse);
    }

    public ListingResponse findById(UUID id) {
        return mapper.toListingResponse(getListing(id));
    }

    @Transactional
    public ListingResponse create(CreateListingRequest request) {
        validateCreateRequest(request);
        listingRepository.findByFlatIdAndStatus(request.flatId(), Listing.ListingStatus.ACTIVE)
                .ifPresent(existing -> {
                    throw new ConflictException("Active listing already exists for flat " + request.flatId());
                });

        Listing listing = Listing.builder()
                .flatId(request.flatId())
                .contractId(request.contractId())
                .listingType(request.listingType())
                .status(Listing.ListingStatus.ACTIVE)
                .priceUsd(request.priceUsd())
                .tokensAvailable(request.tokensTotal())
                .tokensTotal(request.tokensTotal())
                .minInvestmentTokens(request.minInvestmentTokens())
                .title(request.title())
                .description(request.description())
                .sellerInvestorId(request.sellerInvestorId())
                .build();

        Listing saved = listingRepository.save(listing);
        eventPublisher.publishListingCreated(saved);
        return mapper.toListingResponse(saved);
    }

    @Transactional
    public ListingResponse cancel(UUID id) {
        Listing listing = getListing(id);
        if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            throw new ValidationException("Only ACTIVE listings can be cancelled");
        }
        listing.setStatus(Listing.ListingStatus.CANCELLED);
        return mapper.toListingResponse(listing);
    }

    Listing getListing(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + id));
    }

    private static void validateCreateRequest(CreateListingRequest request) {
        if (request.minInvestmentTokens() > request.tokensTotal()) {
            throw new ValidationException("minInvestmentTokens cannot exceed tokensTotal");
        }
    }
}
