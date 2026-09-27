package com.tokenrealty.registry.service;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.InsurancePolicy;
import com.tokenrealty.registry.repository.InsurancePolicyRepository;
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
public class InsuranceService {

    private final InsurancePolicyRepository insurancePolicyRepository;

    public List<InsurancePolicyResponse> findByBuilding(UUID buildingId) {
        return insurancePolicyRepository.findByBuildingId(buildingId).stream()
                .map(this::toResponse)
                .toList();
    }

    public InsurancePolicyResponse findById(UUID id) {
        return toResponse(getById(id));
    }

    @Transactional
    public InsurancePolicyResponse create(CreateInsurancePolicyRequest request) {
        InsurancePolicy policy = insurancePolicyRepository.save(InsurancePolicy.builder()
                .flatId(request.flatId())
                .buildingId(request.buildingId())
                .provider(request.provider())
                .policyNumber(request.policyNumber())
                .coverageUsd(request.coverageUsd())
                .expiresAt(request.expiresAt())
                .status(InsurancePolicy.PolicyStatus.ACTIVE)
                .build());
        return toResponse(policy);
    }

    @Transactional
    public void expireOverduePolicies() {
        insurancePolicyRepository.findAll().stream()
                .filter(p -> p.getStatus() == InsurancePolicy.PolicyStatus.ACTIVE)
                .filter(p -> p.getExpiresAt().isBefore(Instant.now()))
                .forEach(p -> {
                    p.setStatus(InsurancePolicy.PolicyStatus.EXPIRED);
                    insurancePolicyRepository.save(p);
                });
    }

    private InsurancePolicy getById(UUID id) {
        return insurancePolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InsurancePolicy", id));
    }

    private InsurancePolicyResponse toResponse(InsurancePolicy policy) {
        return InsurancePolicyResponse.builder()
                .id(policy.getId())
                .flatId(policy.getFlatId())
                .buildingId(policy.getBuildingId())
                .provider(policy.getProvider())
                .policyNumber(policy.getPolicyNumber())
                .coverageUsd(policy.getCoverageUsd())
                .expiresAt(policy.getExpiresAt())
                .status(policy.getStatus())
                .createdAt(policy.getCreatedAt())
                .build();
    }
}
