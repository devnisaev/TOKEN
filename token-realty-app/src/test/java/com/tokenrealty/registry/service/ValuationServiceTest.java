package com.tokenrealty.registry.service;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.Flat;
import com.tokenrealty.registry.entity.Valuation;
import com.tokenrealty.registry.exception.ResourceNotFoundException;
import com.tokenrealty.registry.mapper.PropertyMapper;
import com.tokenrealty.registry.repository.FlatRepository;
import com.tokenrealty.registry.repository.ValuationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValuationService unit tests")
class ValuationServiceTest {

    @Mock ValuationRepository valuationRepository;
    @Mock FlatRepository flatRepository;
    @Mock PropertyMapper mapper;
    @InjectMocks ValuationService valuationService;

    private UUID flatId;
    private UUID valuationId;
    private Flat flat;
    private Valuation valuation;
    private ValuationResponse valuationResponse;

    @BeforeEach
    void setUp() {
        flatId = UUID.randomUUID();
        valuationId = UUID.randomUUID();

        flat = Flat.builder()
                .flatNumber("101")
                .areaSqm(65.0)
                .status(Flat.FlatStatus.AVAILABLE)
                .build();

        valuation = Valuation.builder()
                .flat(flat)
                .valuationDate(LocalDate.of(2024, 3, 1))
                .valueUsd(BigDecimal.valueOf(75_000))
                .method(Valuation.ValuationMethod.COMPARABLE_SALES)
                .isCurrent(true)
                .build();

        valuationResponse = ValuationResponse.builder()
                .id(valuationId)
                .flatId(flatId)
                .valuationDate(LocalDate.of(2024, 3, 1))
                .valueUsd(BigDecimal.valueOf(75_000))
                .method(Valuation.ValuationMethod.COMPARABLE_SALES)
                .isCurrent(true)
                .build();
    }

    @Test
    @DisplayName("create deactivates previous valuations and saves new one as current")
    void create_deactivatesPreviousAndSavesNew() {
        var request = new CreateValuationRequest(
                LocalDate.of(2024, 6, 1),
                BigDecimal.valueOf(80_000),
                null, null, "John Doe", "APP-001",
                Valuation.ValuationMethod.COMPARABLE_SALES,
                BigDecimal.valueOf(5_000), null);

        when(flatRepository.findById(flatId)).thenReturn(Optional.of(flat));
        when(mapper.toValuation(request)).thenReturn(valuation);
        when(valuationRepository.save(valuation)).thenReturn(valuation);
        when(mapper.toValuationResponse(valuation)).thenReturn(valuationResponse);

        var result = valuationService.create(flatId, request);

        verify(valuationRepository).deactivateCurrentValuations(flatId);
        verify(valuationRepository).save(valuation);
        assertThat(valuation.getIsCurrent()).isTrue();
        assertThat(result.valueUsd()).isEqualByComparingTo(BigDecimal.valueOf(75_000));
    }

    @Test
    @DisplayName("create auto-updates token price when flat has totalTokens set")
    void create_autoUpdatesTokenPrice() {
        flat.setTotalTokens(1000L);

        var request = new CreateValuationRequest(
                LocalDate.now(), BigDecimal.valueOf(50_000),
                null, null, null, null,
                Valuation.ValuationMethod.INCOME_APPROACH, null, null);

        when(flatRepository.findById(flatId)).thenReturn(Optional.of(flat));
        when(mapper.toValuation(request)).thenReturn(valuation);
        when(valuationRepository.save(valuation)).thenReturn(valuation);
        when(mapper.toValuationResponse(valuation)).thenReturn(valuationResponse);

        // Force valueUsd on the mock valuation to match request
        valuation.setValueUsd(BigDecimal.valueOf(50_000));

        valuationService.create(flatId, request);

        // Token price = 50000 / 1000 = 50.00
        assertThat(flat.getTokenPriceUsd()).isEqualByComparingTo(BigDecimal.valueOf(50));
        verify(flatRepository).save(flat);
    }

    @Test
    @DisplayName("findCurrentByFlat returns current valuation")
    void findCurrentByFlat_returnsCurrent() {
        when(flatRepository.existsById(flatId)).thenReturn(true);
        when(valuationRepository.findByFlatIdAndIsCurrentTrue(flatId))
                .thenReturn(Optional.of(valuation));
        when(mapper.toValuationResponse(valuation)).thenReturn(valuationResponse);

        var result = valuationService.findCurrentByFlat(flatId);

        assertThat(result.isCurrent()).isTrue();
        assertThat(result.valueUsd()).isEqualByComparingTo(BigDecimal.valueOf(75_000));
    }

    @Test
    @DisplayName("findCurrentByFlat throws ResourceNotFoundException when no current valuation")
    void findCurrentByFlat_throwsNotFound_whenNoCurrent() {
        when(flatRepository.existsById(flatId)).thenReturn(true);
        when(valuationRepository.findByFlatIdAndIsCurrentTrue(flatId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> valuationService.findCurrentByFlat(flatId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No current valuation");
    }

    @Test
    @DisplayName("findByFlat throws ResourceNotFoundException for unknown flat")
    void findByFlat_throwsNotFound_whenFlatMissing() {
        UUID unknownId = UUID.randomUUID();
        when(flatRepository.existsById(unknownId)).thenReturn(false);

        assertThatThrownBy(() -> valuationService.findByFlat(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete promotes previous valuation to current when deleting current")
    void delete_promotesPreviousWhenDeletingCurrent() {
        Valuation older = Valuation.builder()
                .flat(flat)
                .valuationDate(LocalDate.of(2023, 1, 1))
                .valueUsd(BigDecimal.valueOf(60_000))
                .method(Valuation.ValuationMethod.COMPARABLE_SALES)
                .isCurrent(false)
                .build();

        when(valuationRepository.findById(valuationId)).thenReturn(Optional.of(valuation));
        when(valuationRepository.findByFlatIdOrderByValuationDateDesc(any()))
                .thenReturn(List.of(valuation, older));

        valuationService.delete(valuationId);

        assertThat(older.getIsCurrent()).isTrue();
        verify(valuationRepository).save(older);
        verify(valuationRepository).delete(valuation);
    }
}