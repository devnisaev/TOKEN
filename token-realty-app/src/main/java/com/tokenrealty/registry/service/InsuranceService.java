package com.tokenrealty.registry.service;

import com.tokenrealty.registry.dto.PropertyDtos.*;
import com.tokenrealty.registry.entity.InsurancePolicy;
import com.tokenrealty.registry.repository.InsurancePolicyRepository;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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

    public List<InsuranceExpiryAlertItem> findExpiringWithinDays(int withinDays) {
        Instant now = Instant.now();
        Instant cutoff = now.plus(withinDays, ChronoUnit.DAYS);
        return insurancePolicyRepository
                .findByStatusAndExpiresAtBetween(InsurancePolicy.PolicyStatus.ACTIVE, now, cutoff)
                .stream()
                .map(policy -> toExpiryAlert(policy, now))
                .sorted(Comparator.comparingLong(InsuranceExpiryAlertItem::daysUntilExpiry))
                .toList();
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

    private InsuranceExpiryAlertItem toExpiryAlert(InsurancePolicy policy, Instant now) {
        long days = ChronoUnit.DAYS.between(now, policy.getExpiresAt());
        return new InsuranceExpiryAlertItem(
                policy.getId(),
                policy.getFlatId(),
                policy.getBuildingId(),
                policy.getProvider(),
                policy.getPolicyNumber(),
                policy.getCoverageUsd(),
                policy.getExpiresAt(),
                Math.max(days, 0));
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
