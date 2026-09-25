package com.tokenrealty.registry.service;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.Flat;
import com.tokenrealty.registry.entity.Valuation;
import com.tokenrealty.registry.exception.ResourceNotFoundException;
import com.tokenrealty.registry.mapper.PropertyMapper;
import com.tokenrealty.registry.repository.FlatRepository;
import com.tokenrealty.registry.repository.ValuationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional(readOnly = true)
public class ValuationService {

    private final ValuationRepository valuationRepository;
    private final FlatRepository flatRepository;
    private final PropertyMapper mapper;
    public ValuationService(ValuationRepository valuationRepository, FlatRepository flatRepository, PropertyMapper mapper) {
        this.valuationRepository = valuationRepository;
        this.flatRepository = flatRepository;
        this.mapper = mapper;
    }


    public List<ValuationResponse> findByFlat(UUID flatId) {
        ensureFlatExists(flatId);
        return mapper.toValuationResponses(
                valuationRepository.findByFlatIdOrderByValuationDateDesc(flatId));
    }

    public ValuationResponse findCurrentByFlat(UUID flatId) {
        ensureFlatExists(flatId);
        return valuationRepository.findByFlatIdAndIsCurrentTrue(flatId)
                .map(mapper::toValuationResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No current valuation found for flat " + flatId));
    }

    public ValuationResponse findById(UUID id) {
        return mapper.toValuationResponse(getOrThrow(id));
    }

    @Transactional
    public ValuationResponse create(UUID flatId, CreateValuationRequest request) {
        Flat flat = flatRepository.findById(flatId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat", flatId));

        // Deactivate any previous current valuation
        valuationRepository.deactivateCurrentValuations(flatId);

        Valuation valuation = mapper.toValuation(request);
        valuation.setFlat(flat);
        valuation.setIsCurrent(true);

        // Auto-calculate token price if the flat has total tokens set
        if (flat.getTotalTokens() != null && flat.getTotalTokens() > 0) {
            java.math.BigDecimal tokenPrice = request.valueUsd()
                    .divide(java.math.BigDecimal.valueOf(flat.getTotalTokens()),
                            2, java.math.RoundingMode.HALF_UP);
            flat.setTokenPriceUsd(tokenPrice);
            flatRepository.save(flat);
            log.info("Auto-updated token price to {} for flat {}", tokenPrice, flatId);
        }

        Valuation saved = valuationRepository.save(valuation);
        log.info("Created valuation id={} valueUsd={} for flat={}", saved.getId(), saved.getValueUsd(), flatId);
        return mapper.toValuationResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Valuation valuation = getOrThrow(id);
        if (Boolean.TRUE.equals(valuation.getIsCurrent())) {
            // Promote the most recent non-current valuation to current
            valuationRepository.findByFlatIdOrderByValuationDateDesc(valuation.getFlat().getId())
                    .stream()
                    .filter(v -> v != valuation)  // exclude the one being deleted by reference
                    .findFirst()
                    .ifPresent(v -> {
                        v.setIsCurrent(true);
                        valuationRepository.save(v);
                    });
        }
        valuationRepository.delete(valuation);
        log.info("Deleted valuation id={}", id);
    }

    private Valuation getOrThrow(UUID id) {
        return valuationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Valuation", id));
    }

    private void ensureFlatExists(UUID flatId) {
        if (!flatRepository.existsById(flatId)) {
            throw new ResourceNotFoundException("Flat", flatId);
        }
    }
}