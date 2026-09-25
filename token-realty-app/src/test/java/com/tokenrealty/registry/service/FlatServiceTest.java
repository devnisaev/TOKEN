package com.tokenrealty.registry.service;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.entity.Flat;
import com.tokenrealty.registry.exception.ConflictException;
import com.tokenrealty.registry.exception.ResourceNotFoundException;
import com.tokenrealty.registry.mapper.PropertyMapper;
import com.tokenrealty.registry.repository.BuildingRepository;
import com.tokenrealty.registry.repository.FlatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlatService unit tests")
class FlatServiceTest {

    @Mock FlatRepository flatRepository;
    @Mock BuildingRepository buildingRepository;
    @Mock PropertyMapper mapper;
    @InjectMocks FlatService flatService;

    private UUID buildingId;
    private UUID flatId;
    private Building building;
    private Flat flat;
    private FlatResponse flatResponse;

    @BeforeEach
    void setUp() {
        buildingId = UUID.randomUUID();
        flatId = UUID.randomUUID();

        building = Building.builder()
                .name("Sunrise Tower")
                .address("123 Main St")
                .city("Bishkek")
                .country("KG")
                .status(Building.BuildingStatus.APPROVED)
                .build();

        flat = Flat.builder()
                .flatNumber("101")
                .floor(1)
                .areaSqm(65.0)
                .numRooms(2)
                .numBathrooms(1)
                .status(Flat.FlatStatus.AVAILABLE)
                .building(building)
                .build();

        flatResponse = FlatResponse.builder()
                .id(flatId)
                .buildingId(buildingId)
                .flatNumber("101")
                .floor(1)
                .areaSqm(65.0)
                .status(Flat.FlatStatus.AVAILABLE)
                .build();
    }

    @Test
    @DisplayName("findByBuilding returns paged flats")
    void findByBuilding_returnsMappedPage() {
        when(buildingRepository.existsById(buildingId)).thenReturn(true);
        when(flatRepository.findByBuildingId(eq(buildingId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(flat)));
        when(mapper.toFlatResponse(flat)).thenReturn(flatResponse);

        var result = flatService.findByBuilding(buildingId, Pageable.unpaged());

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).flatNumber()).isEqualTo("101");
    }

    @Test
    @DisplayName("create saves flat under the correct building")
    void create_savesFlat() {
        var request = new CreateFlatRequest("101", 1, 65.0, 2, 1);

        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
        when(flatRepository.existsByBuildingIdAndFlatNumber(buildingId, "101")).thenReturn(false);
        when(mapper.toFlat(request)).thenReturn(flat);
        when(flatRepository.save(flat)).thenReturn(flat);
        when(mapper.toFlatResponse(flat)).thenReturn(flatResponse);

        var result = flatService.create(buildingId, request);

        assertThat(result.flatNumber()).isEqualTo("101");
        verify(flatRepository).save(flat);
        assertThat(flat.getBuilding()).isEqualTo(building);
    }

    @Test
    @DisplayName("create throws ConflictException when flat number duplicate")
    void create_throwsConflict_whenDuplicateFlatNumber() {
        var request = new CreateFlatRequest("101", 1, 65.0, 2, 1);
        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
        when(flatRepository.existsByBuildingIdAndFlatNumber(buildingId, "101")).thenReturn(true);

        assertThatThrownBy(() -> flatService.create(buildingId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("101");

        verify(flatRepository, never()).save(any());
    }

    @Test
    @DisplayName("create throws ConflictException when building is SUSPENDED")
    void create_throwsConflict_whenBuildingSuspended() {
        building.setStatus(Building.BuildingStatus.SUSPENDED);
        var request = new CreateFlatRequest("102", 2, 50.0, 1, 1);
        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));

        assertThatThrownBy(() -> flatService.create(buildingId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("suspended");
    }

    @Test
    @DisplayName("update throws ConflictException for TOKENIZED flat")
    void update_throwsConflict_whenTokenized() {
        flat.setStatus(Flat.FlatStatus.TOKENIZED);
        when(flatRepository.findById(flatId)).thenReturn(Optional.of(flat));

        var request = new UpdateFlatRequest("101A", null, null, null, null);

        assertThatThrownBy(() -> flatService.update(flatId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("tokenized flat");

        verify(flatRepository, never()).save(any());
    }

    @Test
    @DisplayName("setTokenInfo assigns contract address and sets status to TOKENIZED")
    void setTokenInfo_updatesContractAndStatus() {
        when(flatRepository.findById(flatId)).thenReturn(Optional.of(flat));
        when(flatRepository.save(flat)).thenReturn(flat);
        when(mapper.toFlatResponse(flat)).thenReturn(
                new FlatResponse(
                        flatResponse.id(), flatResponse.buildingId(), flatResponse.buildingName(),
                        flatResponse.flatNumber(), flatResponse.floor(), flatResponse.areaSqm(),
                        flatResponse.numRooms(), flatResponse.numBathrooms(),
                        Flat.FlatStatus.TOKENIZED, "0xABC123", 1000L,
                        flatResponse.tokenPriceUsd(), flatResponse.currentValuation(),
                        flatResponse.createdAt(), flatResponse.updatedAt()));

        var result = flatService.setTokenInfo(flatId, "0xABC123", 1000L, BigDecimal.valueOf(50.00));

        assertThat(flat.getTokenContractAddress()).isEqualTo("0xABC123");
        assertThat(flat.getTotalTokens()).isEqualTo(1000L);
        assertThat(flat.getStatus()).isEqualTo(Flat.FlatStatus.TOKENIZED);
        assertThat(building.getStatus()).isEqualTo(Building.BuildingStatus.TOKENIZED);
        assertThat(result.tokenContractAddress()).isEqualTo("0xABC123");
    }

    @Test
    @DisplayName("delete throws ConflictException for FULLY_SOLD flat")
    void delete_throwsConflict_whenFullySold() {
        flat.setStatus(Flat.FlatStatus.FULLY_SOLD);
        when(flatRepository.findById(flatId)).thenReturn(Optional.of(flat));

        assertThatThrownBy(() -> flatService.delete(flatId))
                .isInstanceOf(ConflictException.class);

        verify(flatRepository, never()).delete(any());
    }

    @Test
    @DisplayName("findById throws ResourceNotFoundException for unknown id")
    void findById_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(flatRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flatService.findById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }
}