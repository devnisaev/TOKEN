package com.tokenrealty.registry.service;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.registry.mapper.PropertyMapper;
import com.tokenrealty.registry.repository.BuildingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuildingService unit tests")
class BuildingServiceTest {

    @Mock BuildingRepository buildingRepository;
    @Mock PropertyMapper mapper;
    @InjectMocks BuildingService buildingService;

    private Building building;
    private BuildingResponse buildingResponse;
    private UUID buildingId;

    @BeforeEach
    void setUp() {
        buildingId = UUID.randomUUID();
        building = Building.builder()
                .name("Sunrise Tower")
                .address("123 Main St")
                .city("Bishkek")
                .country("KG")
                .status(Building.BuildingStatus.PENDING_REVIEW)
                .build();

        buildingResponse = BuildingResponse.builder()
                .id(buildingId)
                .name("Sunrise Tower")
                .city("Bishkek")
                .status(Building.BuildingStatus.PENDING_REVIEW)
                .flatCount(0)
                .build();
    }

    @Test
    @DisplayName("findAll returns paged buildings")
    void findAll_returnsMappedPage() {
        when(buildingRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(building)));
        when(mapper.toBuildingResponse(building)).thenReturn(buildingResponse);

        var result = buildingService.findAll(Pageable.unpaged());

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Sunrise Tower");
    }

    @Test
    @DisplayName("create saves and returns building")
    void create_savesBuilding() {
        var request = new CreateBuildingRequest(
                "Sunrise Tower", "123 Main St", "Bishkek", "KG",
                null, 10, 40, 2020, 3500.0, null, null);

        when(buildingRepository.existsByAddressAndCity(any(), any())).thenReturn(false);
        when(mapper.toBuilding(request)).thenReturn(building);
        when(buildingRepository.save(building)).thenReturn(building);
        when(mapper.toBuildingResponse(building)).thenReturn(buildingResponse);

        var result = buildingService.create(request);

        assertThat(result.name()).isEqualTo("Sunrise Tower");
        verify(buildingRepository).save(building);
    }

    @Test
    @DisplayName("create throws ConflictException when address+city duplicate")
    void create_throwsConflict_whenDuplicateAddress() {
        var request = new CreateBuildingRequest(
                "Duplicate", "123 Main St", "Bishkek", "KG",
                null, 5, 20, 2015, 1000.0, null, null);

        when(buildingRepository.existsByAddressAndCity("123 Main St", "Bishkek")).thenReturn(true);

        assertThatThrownBy(() -> buildingService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Bishkek");

        verify(buildingRepository, never()).save(any());
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException when not found")
    void findById_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(buildingRepository.findByIdWithFlats(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> buildingService.findById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    @DisplayName("delete throws ConflictException when building is TOKENIZED")
    void delete_throwsConflict_whenTokenized() {
        building.setStatus(Building.BuildingStatus.TOKENIZED);
        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));

        assertThatThrownBy(() -> buildingService.delete(buildingId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("token contracts");

        verify(buildingRepository, never()).delete(any());
    }

    @Test
    @DisplayName("updateStatus transitions status correctly")
    void updateStatus_updatesAndReturns() {
        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
        when(buildingRepository.save(building)).thenReturn(building);
        when(mapper.toBuildingResponse(building)).thenReturn(
                new BuildingResponse(
                        buildingResponse.id(), buildingResponse.name(), buildingResponse.address(),
                        buildingResponse.city(), buildingResponse.country(), buildingResponse.postalCode(),
                        buildingResponse.totalFloors(), buildingResponse.totalFlats(), buildingResponse.constructionYear(),
                        buildingResponse.totalAreaSqm(), Building.BuildingStatus.APPROVED,
                        buildingResponse.latitude(), buildingResponse.longitude(),
                        buildingResponse.flatCount(), buildingResponse.createdAt(), buildingResponse.updatedAt()));

        var result = buildingService.updateStatus(buildingId, Building.BuildingStatus.APPROVED);

        assertThat(result.status()).isEqualTo(Building.BuildingStatus.APPROVED);
        assertThat(building.getStatus()).isEqualTo(Building.BuildingStatus.APPROVED);
    }
}