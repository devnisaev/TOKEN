package com.tokenrealty.registry.service;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.exception.ConflictException;
import com.tokenrealty.registry.exception.ResourceNotFoundException;
import com.tokenrealty.registry.mapper.PropertyMapper;
import com.tokenrealty.registry.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BuildingService {

    private final PropertyMapper mapper;
    private final BuildingRepository buildingRepository;

    public Page<BuildingResponse> findAll(Pageable pageable) {
        return buildingRepository.findAll(pageable).map(mapper::toBuildingResponse);
    }

    public Page<BuildingResponse> findByStatus(Building.BuildingStatus status, Pageable pageable) {
        return buildingRepository.findByStatus(status, pageable).map(mapper::toBuildingResponse);
    }

    public Page<BuildingResponse> search(String query, Pageable pageable) {
        return buildingRepository.search(query, pageable).map(mapper::toBuildingResponse);
    }

    public BuildingDetailResponse findById(UUID id) {
        Building building = buildingRepository.findByIdWithFlats(id)
                .orElseThrow(() -> new ResourceNotFoundException("Building: " + id));
        return mapper.toBuildingDetailResponse(building);
    }

    @Transactional
    public BuildingResponse create(CreateBuildingRequest request) {
        if (buildingRepository.existsByAddressAndCity(request.address(), request.city())) {
            throw new ConflictException("Building already registered at this address in " + request.city());
        }
        Building building = mapper.toBuilding(request);
        Building saved = buildingRepository.save(building);
        //log.info("Created building id={} name={}", saved.getId(), saved.getName());
        return mapper.toBuildingResponse(saved);
    }

    @Transactional
    public BuildingResponse update(UUID id, UpdateBuildingRequest request) {
        Building building = getOrThrow(id);
        mapper.updateBuildingFromRequest(request, building);
        return mapper.toBuildingResponse(buildingRepository.save(building));
    }

    @Transactional
    public BuildingResponse updateStatus(UUID id, Building.BuildingStatus newStatus) {
        Building building = getOrThrow(id);
        //log.info("Building {} status {} -> {}", id, building.getStatus(), newStatus);
        building.setStatus(newStatus);
        return mapper.toBuildingResponse(buildingRepository.save(building));
    }

    @Transactional
    public void delete(UUID id) {
        Building building = getOrThrow(id);
        if (building.getStatus() == Building.BuildingStatus.TOKENIZED) {
            throw new ConflictException("Cannot delete a building with active token contracts");
        }
        buildingRepository.delete(building);
        //log.info("Deleted building id={}", id);
    }

    private Building getOrThrow(UUID id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Building: " + id));
    }
}