package com.tokenrealty.registry.mapper;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.*;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper.
 *
 * Key configuration choices:
 *
 * 1. builder = @Builder(disableBuilder = true) on every method that returns a
 *    Java record — records use a canonical constructor, not a builder, so we
 *    must disable the builder strategy and let MapStruct call the constructor.
 *
 * 2. All nested entity paths ("building.id" etc.) are replaced with
 *    expression = "java(...)" to avoid MapStruct failing when Lombok getters
 *    haven't been indexed by the processor yet.
 *
 * 3. @BeanMapping(nullValuePropertyMappingStrategy = IGNORE) on update methods
 *    means only non-null fields from the request patch the target entity.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = false)
)
public interface PropertyMapper {

    // ─── Building ───────────────────────────────────────────────────────────

    @BeanMapping(builder = @Builder(disableBuilder = true))
    Building toBuilding(CreateBuildingRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "flatCount",
            expression = "java(building.getFlats() != null ? building.getFlats().size() : 0)")
    BuildingResponse toBuildingResponse(Building building);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "flatCount",
            expression = "java(building.getFlats() != null ? building.getFlats().size() : 0)")
    @Mapping(target = "flats",
            expression = "java(building.getFlats() != null ? toFlatSummaries(building.getFlats()) : java.util.List.of())")
    @Mapping(target = "spv",
            expression = "java(building.getSpv() != null ? toSpvSummary(building.getSpv()) : null)")
    @Mapping(target = "documents",
            expression = "java(building.getDocuments() != null ? toDocumentResponses(building.getDocuments()) : java.util.List.of())")
    BuildingDetailResponse toBuildingDetailResponse(Building building);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateBuildingFromRequest(UpdateBuildingRequest request, @MappingTarget Building building);

    // ─── Flat ───────────────────────────────────────────────────────────────

    @BeanMapping(builder = @Builder(disableBuilder = true))
    Flat toFlat(CreateFlatRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "buildingId",
            expression = "java(flat.getBuilding() != null ? flat.getBuilding().getId() : null)")
    @Mapping(target = "buildingName",
            expression = "java(flat.getBuilding() != null ? flat.getBuilding().getName() : null)")
    @Mapping(target = "currentValuation",
            expression = "java(flat.getValuations() != null ? flat.getValuations().stream().filter(v -> Boolean.TRUE.equals(v.getIsCurrent())).findFirst().map(this::toValuationResponse).orElse(null) : null)")
    FlatResponse toFlatResponse(Flat flat);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    FlatSummary toFlatSummary(Flat flat);

    List<FlatSummary> toFlatSummaries(List<Flat> flats);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFlatFromRequest(UpdateFlatRequest request, @MappingTarget Flat flat);

    // ─── SPV ────────────────────────────────────────────────────────────────

    @BeanMapping(builder = @Builder(disableBuilder = true))
    SpvEntity toSpv(CreateSpvRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "buildingId",
            expression = "java(spv.getBuilding() != null ? spv.getBuilding().getId() : null)")
    SpvResponse toSpvResponse(SpvEntity spv);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    SpvSummary toSpvSummary(SpvEntity spv);

    // ─── Valuation ──────────────────────────────────────────────────────────

    @BeanMapping(builder = @Builder(disableBuilder = true))
    Valuation toValuation(CreateValuationRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "flatId",
            expression = "java(valuation.getFlat() != null ? valuation.getFlat().getId() : null)")
    ValuationResponse toValuationResponse(Valuation valuation);

    List<ValuationResponse> toValuationResponses(List<Valuation> valuations);

    // ─── Document ───────────────────────────────────────────────────────────

    @BeanMapping(builder = @Builder(disableBuilder = true))
    PropertyDocument toDocument(RegisterDocumentRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    DocumentResponse toDocumentResponse(PropertyDocument document);

    List<DocumentResponse> toDocumentResponses(List<PropertyDocument> documents);
}