package com.tokenrealty.registry.service;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.EsgProfile;
import com.tokenrealty.registry.repository.EsgProfileRepository;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EsgService {

    private final EsgProfileRepository esgProfileRepository;

    public List<EsgProfileResponse> findAll() {
        return esgProfileRepository.findAll().stream().map(this::toResponse).toList();
    }

    public EsgProfileResponse findByFlatId(UUID flatId) {
        return esgProfileRepository.findByFlatId(flatId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("EsgProfile for flat", flatId));
    }

    @Transactional
    public EsgProfileResponse upsert(UpsertEsgProfileRequest request) {
        EsgProfile profile = request.flatId() != null
                ? esgProfileRepository.findByFlatId(request.flatId()).orElse(null)
                : null;
        if (profile == null) {
            profile = EsgProfile.builder()
                    .flatId(request.flatId())
                    .buildingId(request.buildingId())
                    .build();
        }
        profile.setCarbonScore(request.carbonScore());
        profile.setEnergyRating(request.energyRating());
        profile.setEnvironmentalRiskTier(request.environmentalRiskTier());
        profile.setLastAssessedAt(Instant.now());
        return toResponse(esgProfileRepository.save(profile));
    }

    private EsgProfileResponse toResponse(EsgProfile profile) {
        return EsgProfileResponse.builder()
                .id(profile.getId())
                .flatId(profile.getFlatId())
                .buildingId(profile.getBuildingId())
                .carbonScore(profile.getCarbonScore())
                .energyRating(profile.getEnergyRating())
                .environmentalRiskTier(profile.getEnvironmentalRiskTier())
                .lastAssessedAt(profile.getLastAssessedAt())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
