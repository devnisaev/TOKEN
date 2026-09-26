package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.client.ComplianceClient;
import com.tokenrealty.marketplace.client.PropertyRegistryClient;
import com.tokenrealty.marketplace.client.TokenIssuanceClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.CreateListingRequest;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.CreateSecondaryListingRequest;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.ListingResponse;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ValidationException;
import com.tokenrealty.marketplace.kafka.port.ListingCreatedPublisher;
import com.tokenrealty.marketplace.mapper.MarketplaceMapper;
import com.tokenrealty.marketplace.repository.ApprovedBuildingRepository;
import com.tokenrealty.marketplace.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ListingService unit tests")
class ListingServiceTest {

    @Mock ListingRepository listingRepository;
    @Mock ApprovedBuildingRepository approvedBuildingRepository;
    @Mock MarketplaceMapper mapper;
    @Mock ListingCreatedPublisher listingCreatedPublisher;
    @Mock PropertyRegistryClient propertyRegistryClient;
    @Mock TokenIssuanceClient tokenIssuanceClient;
    @Mock ComplianceClient complianceClient;
    @InjectMocks ListingService listingService;

    private UUID flatId;
    private UUID buildingId;

    @BeforeEach
    void setUp() {
        flatId = UUID.randomUUID();
        buildingId = UUID.randomUUID();
        when(propertyRegistryClient.getFlat(flatId)).thenReturn(
                new PropertyRegistryClient.FlatView(flatId, buildingId, "Tower", "101", 1, 50.0, "TOKENIZED"));
        when(propertyRegistryClient.getSpvByBuilding(buildingId)).thenReturn(
                new PropertyRegistryClient.SpvView(
                        UUID.randomUUID(), buildingId, "SPV", "REG-1", "0xspv",
                        "SPV_SHARE_EQUITY", true, "ACTIVE"));
    }

    @Test
    @DisplayName("create saves active listing")
    void createSavesListing() {
        CreateListingRequest request = CreateListingRequest.builder()
                .flatId(flatId)
                .listingType(Listing.ListingType.PRIMARY)
                .priceUsd(new BigDecimal("100.00"))
                .tokensTotal(1000L)
                .minInvestmentTokens(10L)
                .title("Sunrise Tower Flat 12")
                .build();

        Listing saved = Listing.builder()
                .flatId(flatId)
                .listingType(Listing.ListingType.PRIMARY)
                .instrumentType(Listing.InstrumentType.EQUITY)
                .status(Listing.ListingStatus.ACTIVE)
                .priceUsd(new BigDecimal("100.00"))
                .tokensAvailable(1000L)
                .tokensTotal(1000L)
                .minInvestmentTokens(10L)
                .build();
        saved.setId(UUID.randomUUID());

        when(listingRepository.findByFlatIdAndStatus(flatId, Listing.ListingStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(listingRepository.save(any(Listing.class))).thenReturn(saved);
        when(mapper.toListingResponse(saved)).thenReturn(ListingResponse.builder().id(saved.getId()).flatId(flatId).build());

        ListingResponse response = listingService.create(request);

        assertThat(response.id()).isEqualTo(saved.getId());
        verify(listingCreatedPublisher).publishListingCreated(any());
    }

    @Test
    @DisplayName("create rejects duplicate active listing")
    void createRejectsDuplicate() {
        CreateListingRequest request = CreateListingRequest.builder()
                .flatId(flatId)
                .listingType(Listing.ListingType.PRIMARY)
                .priceUsd(new BigDecimal("100.00"))
                .tokensTotal(1000L)
                .minInvestmentTokens(10L)
                .build();

        when(listingRepository.findByFlatIdAndStatus(flatId, Listing.ListingStatus.ACTIVE))
                .thenReturn(Optional.of(new Listing()));

        assertThatThrownBy(() -> listingService.create(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("create allows primary listing for debt instrument SPV")
    void createAllowsPrimaryForDebtInstrument() {
        when(propertyRegistryClient.getSpvByBuilding(buildingId)).thenReturn(
                new PropertyRegistryClient.SpvView(
                        UUID.randomUUID(), buildingId, "SPV", "REG-1", "0xspv",
                        "PART_DEBT_INSTRUMENT", true, "ACTIVE"));

        CreateListingRequest request = CreateListingRequest.builder()
                .flatId(flatId)
                .listingType(Listing.ListingType.PRIMARY)
                .priceUsd(new BigDecimal("100.00"))
                .tokensTotal(100L)
                .minInvestmentTokens(10L)
                .build();

        Listing saved = Listing.builder()
                .flatId(flatId)
                .listingType(Listing.ListingType.PRIMARY)
                .instrumentType(Listing.InstrumentType.DEBT_INSTRUMENT)
                .status(Listing.ListingStatus.ACTIVE)
                .priceUsd(new BigDecimal("100.00"))
                .tokensAvailable(100L)
                .tokensTotal(100L)
                .minInvestmentTokens(10L)
                .build();
        saved.setId(UUID.randomUUID());

        when(listingRepository.findByFlatIdAndStatus(flatId, Listing.ListingStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing listing = invocation.getArgument(0);
            assertThat(listing.getInstrumentType()).isEqualTo(Listing.InstrumentType.DEBT_INSTRUMENT);
            return saved;
        });
        when(mapper.toListingResponse(saved)).thenReturn(ListingResponse.builder().id(saved.getId()).flatId(flatId).build());

        listingService.create(request);

        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    @DisplayName("createSecondary rejects non-equity instrument type")
    void createSecondaryRejectsNonEquity() {
        when(propertyRegistryClient.getSpvByBuilding(buildingId)).thenReturn(
                new PropertyRegistryClient.SpvView(
                        UUID.randomUUID(), buildingId, "SPV", "REG-1", "0xspv",
                        "PART_DEBT_INSTRUMENT", true, "ACTIVE"));

        CreateSecondaryListingRequest request = CreateSecondaryListingRequest.builder()
                .flatId(flatId)
                .contractId(UUID.randomUUID())
                .sellerInvestorId(UUID.randomUUID())
                .sellerWallet("0xSeller")
                .priceUsd(new BigDecimal("50.00"))
                .tokenAmount(10L)
                .build();

        assertThatThrownBy(() -> listingService.createSecondary(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("equity instrument type");
    }

    @Test
    @DisplayName("create validates min investment")
    void createValidatesMinInvestment() {
        CreateListingRequest request = CreateListingRequest.builder()
                .flatId(flatId)
                .listingType(Listing.ListingType.PRIMARY)
                .priceUsd(new BigDecimal("100.00"))
                .tokensTotal(100L)
                .minInvestmentTokens(200L)
                .build();

        assertThatThrownBy(() -> listingService.create(request))
                .isInstanceOf(ValidationException.class);
    }
}
