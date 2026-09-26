package com.tokenrealty.registry.service;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.entity.SpvEntity;
import com.tokenrealty.registry.kafka.port.BuildingApprovedPublisher;
import com.tokenrealty.registry.kafka.port.BuildingApprovedPublisher.BuildingApprovedEvent;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.registry.mapper.PropertyMapper;
import com.tokenrealty.registry.repository.BuildingRepository;
import com.tokenrealty.registry.repository.SpvRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@Transactional(readOnly = true)
public class SpvService {

    private final SpvRepository spvRepository;
    private final BuildingRepository buildingRepository;
    private final PropertyMapper mapper;
    private final BuildingApprovedPublisher buildingApprovedPublisher;

    public SpvService(SpvRepository spvRepository, BuildingRepository buildingRepository,
                      PropertyMapper mapper, BuildingApprovedPublisher buildingApprovedPublisher) {
        this.spvRepository = spvRepository;
        this.buildingRepository = buildingRepository;
        this.mapper = mapper;
        this.buildingApprovedPublisher = buildingApprovedPublisher;
    }


    public SpvResponse findByBuilding(UUID buildingId) {
        return spvRepository.findByBuildingId(buildingId)
                .map(mapper::toSpvResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SPV not found for building " + buildingId));
    }

    public SpvResponse findById(UUID id) {
        return mapper.toSpvResponse(getOrThrow(id));
    }

    @Transactional
    public SpvResponse create(UUID buildingId, CreateSpvRequest request) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building", buildingId));

        if (spvRepository.findByBuildingId(buildingId).isPresent()) {
            throw new ConflictException("Building " + buildingId + " already has a registered SPV");
        }
        if (spvRepository.existsByRegistrationNumber(request.registrationNumber())) {
            throw new ConflictException(
                    "SPV with registration number " + request.registrationNumber() + " already exists");
        }

        SpvEntity spv = mapper.toSpv(request);
        spv.setBuilding(building);
        SpvEntity saved = spvRepository.save(spv);

        if (building.getStatus() == Building.BuildingStatus.PENDING_REVIEW) {
            building.setStatus(Building.BuildingStatus.APPROVED);
            buildingRepository.save(building);
            buildingApprovedPublisher.publishBuildingApproved(
                    new BuildingApprovedEvent(buildingId, Instant.now(), "spv-registration"));
        }

        log.info("Created SPV id={} legal={} for building={}", saved.getId(), saved.getLegalName(), buildingId);
        return mapper.toSpvResponse(saved);
    }

    @Transactional
    public SpvResponse updateWalletAddress(UUID id, String walletAddress) {
        SpvEntity spv = getOrThrow(id);

        // Ensure wallet address is not already taken by another SPV
        spvRepository.findByWalletAddress(walletAddress).ifPresent(existing -> {
            // Guard against null id (e.g. unsaved entity in tests)
            if (existing.getId() == null || !existing.getId().equals(id)) {
                throw new ConflictException("Wallet address already registered to another SPV");
            }
        });

        spv.setWalletAddress(walletAddress);
        log.info("Wallet address updated for SPV {}", id);
        return mapper.toSpvResponse(spvRepository.save(spv));
    }

    @Transactional
    public SpvResponse verifyKyc(UUID id, boolean verified) {
        SpvEntity spv = getOrThrow(id);
        spv.setKycVerified(verified);
        if (verified && spv.getStatus() == SpvEntity.SpvStatus.PENDING) {
            spv.setStatus(SpvEntity.SpvStatus.ACTIVE);
        }
        log.info("SPV {} KYC verification set to {}", id, verified);
        return mapper.toSpvResponse(spvRepository.save(spv));
    }

    @Transactional
    public SpvResponse updateStatus(UUID id, SpvEntity.SpvStatus newStatus) {
        SpvEntity spv = getOrThrow(id);
        log.info("SPV {} status {} -> {}", id, spv.getStatus(), newStatus);
        spv.setStatus(newStatus);
        return mapper.toSpvResponse(spvRepository.save(spv));
    }

    private SpvEntity getOrThrow(UUID id) {
        return spvRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SPV", id));
    }
}