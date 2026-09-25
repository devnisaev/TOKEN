package com.tokenrealty.registry.service;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.entity.SpvEntity;
import com.tokenrealty.registry.exception.ConflictException;
import com.tokenrealty.registry.exception.ResourceNotFoundException;
import com.tokenrealty.registry.mapper.PropertyMapper;
import com.tokenrealty.registry.repository.BuildingRepository;
import com.tokenrealty.registry.repository.SpvRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpvService unit tests")
class SpvServiceTest {

    @Mock SpvRepository spvRepository;
    @Mock BuildingRepository buildingRepository;
    @Mock PropertyMapper mapper;
    @InjectMocks SpvService spvService;

    private UUID buildingId;
    private UUID spvId;
    private Building building;
    private SpvEntity spv;
    private SpvResponse spvResponse;

    @BeforeEach
    void setUp() {
        buildingId = UUID.randomUUID();
        spvId = UUID.randomUUID();

        building = Building.builder()
                .name("Sunrise Tower")
                .address("123 Main St")
                .city("Bishkek")
                .country("KG")
                .status(Building.BuildingStatus.PENDING_REVIEW)
                .build();

        spv = SpvEntity.builder()
                .building(building)
                .legalName("Sunrise SPV LLC")
                .registrationNumber("KG-2024-001")
                .registrationCountry("KG")
                .status(SpvEntity.SpvStatus.PENDING)
                .kycVerified(false)
                .build();

        spvResponse = SpvResponse.builder()
                .id(spvId)
                .buildingId(buildingId)
                .legalName("Sunrise SPV LLC")
                .registrationNumber("KG-2024-001")
                .kycVerified(false)
                .status(SpvEntity.SpvStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("create registers SPV and promotes building to APPROVED")
    void create_registersSpvAndPromotesBuilding() {
        var request = new CreateSpvRequest(
                "Sunrise SPV LLC", "KG-2024-001", "KG",
                LocalDate.of(2024, 1, 15), "123 Main St", null, "TAX-001");

        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
        when(spvRepository.findByBuildingId(buildingId)).thenReturn(Optional.empty());
        when(spvRepository.existsByRegistrationNumber("KG-2024-001")).thenReturn(false);
        when(mapper.toSpv(request)).thenReturn(spv);
        when(spvRepository.save(spv)).thenReturn(spv);
        when(mapper.toSpvResponse(spv)).thenReturn(spvResponse);

        var result = spvService.create(buildingId, request);

        assertThat(result.legalName()).isEqualTo("Sunrise SPV LLC");
        // Building should be promoted to APPROVED
        assertThat(building.getStatus()).isEqualTo(Building.BuildingStatus.APPROVED);
        verify(buildingRepository).save(building);
    }

    @Test
    @DisplayName("create throws ConflictException when building already has SPV")
    void create_throwsConflict_whenSpvAlreadyExists() {
        var request = new CreateSpvRequest(
                "Another SPV", "KG-2024-002", "KG", null, null, null, null);

        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
        when(spvRepository.findByBuildingId(buildingId)).thenReturn(Optional.of(spv));

        assertThatThrownBy(() -> spvService.create(buildingId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has a registered SPV");

        verify(spvRepository, never()).save(any());
    }

    @Test
    @DisplayName("create throws ConflictException on duplicate registration number")
    void create_throwsConflict_whenDuplicateRegNumber() {
        var request = new CreateSpvRequest(
                "Dupe SPV", "KG-2024-001", "KG", null, null, null, null);

        when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
        when(spvRepository.findByBuildingId(buildingId)).thenReturn(Optional.empty());
        when(spvRepository.existsByRegistrationNumber("KG-2024-001")).thenReturn(true);

        assertThatThrownBy(() -> spvService.create(buildingId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("KG-2024-001");
    }

    @Test
    @DisplayName("verifyKyc sets kycVerified=true and activates SPV")
    void verifyKyc_activatesSpv() {
        when(spvRepository.findById(spvId)).thenReturn(Optional.of(spv));
        when(spvRepository.save(spv)).thenReturn(spv);
        when(mapper.toSpvResponse(spv)).thenReturn(
                new SpvResponse(
                        spvResponse.id(), spvResponse.buildingId(), spvResponse.legalName(),
                        spvResponse.registrationNumber(), spvResponse.registrationCountry(),
                        spvResponse.registrationDate(), spvResponse.registeredAddress(),
                        spvResponse.walletAddress(), spvResponse.taxId(),
                        true, SpvEntity.SpvStatus.ACTIVE, spvResponse.createdAt()));

        var result = spvService.verifyKyc(spvId, true);

        assertThat(spv.getKycVerified()).isTrue();
        assertThat(spv.getStatus()).isEqualTo(SpvEntity.SpvStatus.ACTIVE);
        assertThat(result.kycVerified()).isTrue();
    }

    @Test
    @DisplayName("verifyKyc with false keeps SPV in PENDING")
    void verifyKyc_falseKeepsPending() {
        when(spvRepository.findById(spvId)).thenReturn(Optional.of(spv));
        when(spvRepository.save(spv)).thenReturn(spv);
        when(mapper.toSpvResponse(spv)).thenReturn(spvResponse);

        spvService.verifyKyc(spvId, false);

        assertThat(spv.getKycVerified()).isFalse();
        assertThat(spv.getStatus()).isEqualTo(SpvEntity.SpvStatus.PENDING);
    }

    @Test
    @DisplayName("updateWalletAddress throws ConflictException when wallet taken by another SPV")
    void updateWalletAddress_throwsConflict_whenWalletTaken() {
        SpvEntity otherSpv = SpvEntity.builder()
                .legalName("Other SPV")
                .registrationNumber("KG-2024-999")
                .build();
        // Simulate different id
        when(spvRepository.findById(spvId)).thenReturn(Optional.of(spv));
        when(spvRepository.findByWalletAddress("0xDEADBEEF")).thenReturn(Optional.of(otherSpv));

        assertThatThrownBy(() -> spvService.updateWalletAddress(spvId, "0xDEADBEEF"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Wallet address already registered");
    }

    @Test
    @DisplayName("findByBuilding throws ResourceNotFoundException when no SPV exists")
    void findByBuilding_throwsNotFound() {
        when(spvRepository.findByBuildingId(buildingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spvService.findByBuilding(buildingId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}