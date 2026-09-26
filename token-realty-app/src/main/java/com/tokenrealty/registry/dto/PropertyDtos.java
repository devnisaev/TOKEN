package com.tokenrealty.registry.dto;

import com.tokenrealty.registry.entity.*;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PropertyDtos {

    // ─── Building ───────────────────────────────────────────────────────────

    @Builder
    public record CreateBuildingRequest(
            @NotBlank String name,
            @NotBlank String address,
            @NotBlank String city,
            @NotBlank String country,
            String postalCode,
            @Min(1) Integer totalFloors,
            @Min(1) Integer totalFlats,
            @Min(1800) @Max(2100) Integer constructionYear,
            @Positive Double totalAreaSqm,
            Double latitude,
            Double longitude,
            Building.PropertyCategory propertyCategory,
            @Size(max = 100) String cadastralReference,
            @Size(max = 20) String energyEfficiencyRating,
            @Size(max = 50) String zoningCode,
            @Min(1800) @Max(2100) Integer lastRenovationYear
    ) {}

    @Builder
    public record UpdateBuildingRequest(
            String name,
            String address,
            String city,
            String country,
            String postalCode,
            @Min(1) Integer totalFloors,
            @Positive Double totalAreaSqm,
            Double latitude,
            Double longitude,
            Building.PropertyCategory propertyCategory,
            String cadastralReference,
            String energyEfficiencyRating,
            String zoningCode,
            Integer lastRenovationYear
    ) {}

    @Builder
    public record BuildingResponse(
            UUID id,
            String name,
            String address,
            String city,
            String country,
            String postalCode,
            Integer totalFloors,
            Integer totalFlats,
            Integer constructionYear,
            Double totalAreaSqm,
            Building.BuildingStatus status,
            Building.PropertyCategory propertyCategory,
            String cadastralReference,
            String energyEfficiencyRating,
            String zoningCode,
            Integer lastRenovationYear,
            Double latitude,
            Double longitude,
            int flatCount,
            Instant createdAt,
            Instant updatedAt
    ) {}

    @Builder
    public record BuildingDetailResponse(
            UUID id,
            String name,
            String address,
            String city,
            String country,
            String postalCode,
            Integer totalFloors,
            Integer totalFlats,
            Integer constructionYear,
            Double totalAreaSqm,
            Building.BuildingStatus status,
            Building.PropertyCategory propertyCategory,
            String cadastralReference,
            String energyEfficiencyRating,
            String zoningCode,
            Integer lastRenovationYear,
            Double latitude,
            Double longitude,
            int flatCount,
            List<FlatSummary> flats,
            SpvSummary spv,
            List<DocumentResponse> documents,
            Instant createdAt,
            Instant updatedAt
    ) {}

    // ─── Flat ───────────────────────────────────────────────────────────────

    @Builder
    public record CreateFlatRequest(
            @NotBlank String flatNumber,
            @Min(0) Integer floor,
            @Positive Double areaSqm,
            @Min(1) Integer numRooms,
            @Min(1) Integer numBathrooms,
            @Size(max = 100) String cadastralReference,
            @Positive Double netUsableAreaSqm
    ) {}

    @Builder
    public record UpdateFlatRequest(
            String flatNumber,
            Integer floor,
            @Positive Double areaSqm,
            Integer numRooms,
            Integer numBathrooms,
            String cadastralReference,
            Double netUsableAreaSqm
    ) {}

    @Builder
    public record FlatResponse(
            UUID id,
            UUID buildingId,
            String buildingName,
            String flatNumber,
            Integer floor,
            Double areaSqm,
            Double netUsableAreaSqm,
            String cadastralReference,
            Integer numRooms,
            Integer numBathrooms,
            Flat.FlatStatus status,
            String tokenContractAddress,
            Long totalTokens,
            BigDecimal tokenPriceUsd,
            ValuationResponse currentValuation,
            Instant createdAt,
            Instant updatedAt
    ) {}

    @Builder
    public record FlatSummary(
            UUID id,
            String flatNumber,
            Integer floor,
            Double areaSqm,
            Flat.FlatStatus status,
            BigDecimal tokenPriceUsd
    ) {}

    // ─── SPV ────────────────────────────────────────────────────────────────

    //@Builder
    public record CreateSpvRequest(
            @NotBlank String legalName,
            @NotBlank String registrationNumber,
            @NotBlank String registrationCountry,
            LocalDate registrationDate,
            String registeredAddress,
            String walletAddress,
            String taxId,
            @Size(max = 100) String legalJurisdiction,
            SpvEntity.OwnershipType ownershipType
    ) {}

    @Builder
    public record SpvResponse(
            UUID id,
            UUID buildingId,
            String legalName,
            String registrationNumber,
            String registrationCountry,
            LocalDate registrationDate,
            String registeredAddress,
            String walletAddress,
            String taxId,
            String legalJurisdiction,
            SpvEntity.OwnershipType ownershipType,
            Boolean kycVerified,
            SpvEntity.SpvStatus status,
            Instant createdAt
    ) {}

    @Builder
    public record SpvSummary(
            UUID id,
            String legalName,
            String registrationNumber,
            Boolean kycVerified,
            SpvEntity.SpvStatus status
    ) {}

    // ─── Valuation ──────────────────────────────────────────────────────────

    @Builder
    public record CreateValuationRequest(
            @NotNull LocalDate valuationDate,
            @NotNull @Positive BigDecimal valueUsd,
            BigDecimal valueLocalCurrency,
            @Size(min = 3, max = 3) String localCurrency,
            String appraiserName,
            String appraiserLicense,
            @NotNull Valuation.ValuationMethod method,
            @Positive BigDecimal annualRentalIncomeUsd,
            @Positive BigDecimal operatingExpensesEstimateUsd,
            @Positive @DecimalMax("100") BigDecimal targetRentalYieldPct,
            String notes
    ) {}

    @Builder
    public record ValuationResponse(
            UUID id,
            UUID flatId,
            LocalDate valuationDate,
            BigDecimal valueUsd,
            BigDecimal valueLocalCurrency,
            String localCurrency,
            String appraiserName,
            Valuation.ValuationMethod method,
            BigDecimal annualRentalIncomeUsd,
            BigDecimal operatingExpensesEstimateUsd,
            BigDecimal targetRentalYieldPct,
            Boolean isCurrent,
            String notes,
            Instant createdAt
    ) {}

    // ─── Document ───────────────────────────────────────────────────────────

    @Builder
    public record RegisterDocumentRequest(
            @NotBlank String documentName,
            @NotNull PropertyDocument.DocumentType documentType,
            String ipfsCid,
            String storageUrl,
            Long fileSizeBytes,
            String mimeType
    ) {}

    @Builder
    public record DocumentResponse(
            UUID id,
            String documentName,
            PropertyDocument.DocumentType documentType,
            String ipfsCid,
            String storageUrl,
            Long fileSizeBytes,
            String mimeType,
            Boolean isVerified,
            String verifiedBy,
            String uploadedBy,
            Instant createdAt
    ) {}
}