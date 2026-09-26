package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.entity.ApprovedBuilding;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import com.tokenrealty.marketplace.client.ComplianceClient;
import com.tokenrealty.marketplace.client.PropertyRegistryClient;
import com.tokenrealty.marketplace.client.TokenIssuanceClient;
import com.tokenrealty.marketplace.kafka.command.BuildingApprovedCommand;
import com.tokenrealty.marketplace.kafka.command.FlatTokenizedCommand;
import com.tokenrealty.marketplace.kafka.port.ListingCreatedPublisher;
import com.tokenrealty.marketplace.mapper.MarketplaceMapper;
import com.tokenrealty.marketplace.repository.ApprovedBuildingRepository;
import com.tokenrealty.marketplace.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ListingService {

    private final ListingRepository listingRepository;
    private final ApprovedBuildingRepository approvedBuildingRepository;
    private final MarketplaceMapper mapper;
    private final ListingCreatedPublisher listingCreatedPublisher;
    private final TokenIssuanceClient tokenIssuanceClient;
    private final ComplianceClient complianceClient;
    private final PropertyRegistryClient propertyRegistryClient;

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
        Listing.InstrumentType instrumentType = resolveInstrumentType(request.flatId());
        if (request.listingType() == Listing.ListingType.PRIMARY) {
            listingRepository.findByFlatIdAndStatus(request.flatId(), Listing.ListingStatus.ACTIVE)
                    .ifPresent(existing -> {
                        throw new ConflictException("Active listing already exists for flat " + request.flatId());
                    });
        }

        Listing listing = Listing.builder()
                .flatId(request.flatId())
                .contractId(request.contractId())
                .listingType(request.listingType())
                .instrumentType(instrumentType)
                .status(Listing.ListingStatus.ACTIVE)
                .priceUsd(request.priceUsd())
                .tokensAvailable(request.tokensTotal())
                .tokensTotal(request.tokensTotal())
                .minInvestmentTokens(request.minInvestmentTokens())
                .title(request.title())
                .description(request.description())
                .sellerInvestorId(request.sellerInvestorId())
                .sellerWallet(request.sellerWallet())
                .build();

        Listing saved = listingRepository.save(listing);
        listingCreatedPublisher.publishListingCreated(new ListingCreatedPublisher.ListingCreatedEvent(
                saved.getId(),
                saved.getFlatId(),
                saved.getListingType().name(),
                saved.getPriceUsd(),
                saved.getTokensAvailable()));
        return mapper.toListingResponse(saved);
    }

    @Transactional
    public ListingResponse createFromFlatTokenized(FlatTokenizedCommand command) {
        if (!approvedBuildingRepository.existsById(command.buildingId())) {
            log.info("Skipping auto-listing for flat {} — building {} not approved yet",
                    command.flatId(), command.buildingId());
            return null;
        }
        var existing = listingRepository.findByFlatIdAndStatus(command.flatId(), Listing.ListingStatus.ACTIVE);
        if (existing.isPresent()) {
            return mapper.toListingResponse(existing.get());
        }
        TokenIssuanceClient.TokenContractResponse contract = tokenIssuanceClient.getContractByFlatId(command.flatId());
        CreateListingRequest request = CreateListingRequest.builder()
                .flatId(command.flatId())
                .contractId(contract.id())
                .listingType(Listing.ListingType.PRIMARY)
                .priceUsd(command.tokenPriceUsd())
                .tokensTotal(command.totalTokens())
                .minInvestmentTokens(1L)
                .title("Tokenized flat " + command.flatId())
                .description("Auto-created from flat.tokenized event")
                .build();
        return create(request);
    }

    @Transactional
    public void recordBuildingApproved(BuildingApprovedCommand command) {
        if (approvedBuildingRepository.existsById(command.buildingId())) {
            return;
        }
        approvedBuildingRepository.save(ApprovedBuilding.builder()
                .buildingId(command.buildingId())
                .approvedAt(command.approvedAt())
                .approvedBy(command.approvedBy())
                .build());
    }

    @Transactional
    public ListingResponse createSecondary(CreateSecondaryListingRequest request) {
        if (resolveInstrumentType(request.flatId()) != Listing.InstrumentType.EQUITY) {
            throw new ValidationException("Secondary listings require equity instrument type");
        }
        if (!complianceClient.isWalletApproved(request.sellerWallet())) {
            throw new ValidationException("Seller wallet is not KYC approved");
        }
        long balance = tokenIssuanceClient.getHolderBalance(request.contractId(), request.sellerWallet());
        if (balance < request.tokenAmount()) {
            throw new ValidationException("Insufficient token balance for secondary listing");
        }

        CreateListingRequest listingRequest = CreateListingRequest.builder()
                .flatId(request.flatId())
                .contractId(request.contractId())
                .listingType(Listing.ListingType.SECONDARY)
                .priceUsd(request.priceUsd())
                .tokensTotal(request.tokenAmount())
                .minInvestmentTokens(1L)
                .title(request.title() != null ? request.title() : "Secondary listing")
                .description(request.description())
                .sellerInvestorId(request.sellerInvestorId())
                .sellerWallet(request.sellerWallet())
                .build();
        return create(listingRequest);
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

    private Listing.InstrumentType resolveInstrumentType(UUID flatId) {
        var flat = propertyRegistryClient.getFlat(flatId);
        var spv = propertyRegistryClient.getSpvByBuilding(flat.buildingId());
        return switch (spv.ownershipType()) {
            case "SPV_SHARE_EQUITY" -> Listing.InstrumentType.EQUITY;
            case "PROFIT_SHARING_AGREEMENT" -> Listing.InstrumentType.PROFIT_SHARING;
            case "PART_DEBT_INSTRUMENT" -> Listing.InstrumentType.DEBT_INSTRUMENT;
            default -> raiseValidation("Unsupported SPV ownership type: " + spv.ownershipType());
        };
    }

    private static Listing.InstrumentType raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
