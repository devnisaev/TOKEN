package com.tokenrealty.registry.service;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.entity.Flat;
import com.tokenrealty.registry.exception.ConflictException;
import com.tokenrealty.registry.exception.ResourceNotFoundException;
import com.tokenrealty.registry.mapper.PropertyMapper;
import com.tokenrealty.registry.kafka.port.FlatTokenizedPublisher;
import com.tokenrealty.registry.repository.BuildingRepository;
import com.tokenrealty.registry.repository.FlatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FlatService {

    private final FlatRepository flatRepository;
    private final BuildingRepository buildingRepository;
    private final PropertyMapper mapper;
    private final FlatTokenizedPublisher flatTokenizedPublisher;

    public Page<FlatResponse> findByBuilding(UUID buildingId, Pageable pageable) {
        ensureBuildingExists(buildingId);
        return flatRepository.findByBuildingId(buildingId, pageable)
                .map(mapper::toFlatResponse);
    }

    public Page<FlatResponse> findByStatus(Flat.FlatStatus status, Pageable pageable) {
        return flatRepository.findByStatus(status, pageable)
                .map(mapper::toFlatResponse);
    }

    public FlatResponse findById(UUID id) {
        return mapper.toFlatResponse(getOrThrow(id));
    }

    public FlatResponse findByBuildingAndFlatNumber(UUID buildingId, String flatNumber) {
        return flatRepository.findByBuildingIdAndFlatNumber(buildingId, flatNumber)
                .map(mapper::toFlatResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Flat " + flatNumber + " not found in building " + buildingId));
    }

    public List<FlatResponse> findAvailableByBuilding(UUID buildingId) {
        ensureBuildingExists(buildingId);
        return flatRepository
                .findByBuildingIdAndStatus(buildingId, Flat.FlatStatus.AVAILABLE)
                .stream()
                .map(mapper::toFlatResponse)
                .toList();
    }

    @Transactional
    public FlatResponse create(UUID buildingId, CreateFlatRequest request) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building", buildingId));

        if (building.getStatus() == Building.BuildingStatus.SUSPENDED) {
            throw new ConflictException("Cannot add flats to a suspended building");
        }
        if (flatRepository.existsByBuildingIdAndFlatNumber(buildingId, request.flatNumber())) {
            throw new ConflictException(
                    "Flat number " + request.flatNumber() + " already exists in this building");
        }

        Flat flat = mapper.toFlat(request);
        flat.setBuilding(building);
        Flat saved = flatRepository.save(flat);

        log.info("Created flat id={} number={} in building={}", saved.getId(), saved.getFlatNumber(), buildingId);
        return mapper.toFlatResponse(saved);
    }

    @Transactional
    public FlatResponse update(UUID id, UpdateFlatRequest request) {
        Flat flat = getOrThrow(id);
        if (flat.getStatus() == Flat.FlatStatus.TOKENIZED) {
            throw new ConflictException("Cannot update a tokenized flat — changes must go through governance");
        }
        mapper.updateFlatFromRequest(request, flat);
        return mapper.toFlatResponse(flatRepository.save(flat));
    }

    @Transactional
    public FlatResponse updateStatus(UUID id, Flat.FlatStatus newStatus) {
        Flat flat = getOrThrow(id);
        log.info("Flat {} status {} -> {}", id, flat.getStatus(), newStatus);
        flat.setStatus(newStatus);

        // If flat is tokenized, also mark building as tokenized
        if (newStatus == Flat.FlatStatus.TOKENIZED) {
            flat.getBuilding().setStatus(Building.BuildingStatus.TOKENIZED);
        }
        return mapper.toFlatResponse(flatRepository.save(flat));
    }

    @Transactional
    public FlatResponse setTokenInfo(UUID id, String contractAddress, Long totalTokens,
                                     java.math.BigDecimal tokenPriceUsd) {
        Flat flat = getOrThrow(id);
        if (flat.getStatus() != Flat.FlatStatus.AVAILABLE) {
            throw new ConflictException("Token info can only be set for AVAILABLE flats");
        }
        flat.setTokenContractAddress(contractAddress);
        flat.setTotalTokens(totalTokens);
        flat.setTokenPriceUsd(tokenPriceUsd);
        flat.setStatus(Flat.FlatStatus.TOKENIZED);
        flat.getBuilding().setStatus(Building.BuildingStatus.TOKENIZED);

        Flat saved = flatRepository.save(flat);
        flatTokenizedPublisher.publishFlatTokenized(new FlatTokenizedPublisher.FlatTokenizedEvent(
                saved.getId(),
                saved.getBuilding().getId(),
                contractAddress,
                totalTokens,
                tokenPriceUsd));
        log.info("Token contract {} assigned to flat {}", contractAddress, id);
        return mapper.toFlatResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Flat flat = getOrThrow(id);
        if (flat.getStatus() == Flat.FlatStatus.TOKENIZED || flat.getStatus() == Flat.FlatStatus.FULLY_SOLD) {
            throw new ConflictException("Cannot delete a flat with active token contracts");
        }
        flatRepository.delete(flat);
        log.info("Deleted flat id={}", id);
    }

    private Flat getOrThrow(UUID id) {
        return flatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flat", id));
    }

    private void ensureBuildingExists(UUID buildingId) {
        if (!buildingRepository.existsById(buildingId)) {
            throw new ResourceNotFoundException("Building", buildingId);
        }
    }
}