package com.tokenrealty.compliance.service;

import com.tokenrealty.compliance.dto.ComplianceDtos.*;
import com.tokenrealty.compliance.entity.ComplianceRecord;
import com.tokenrealty.compliance.kafka.port.KycEventPublisher;
import com.tokenrealty.compliance.repository.ComplianceRecordRepository;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplianceService {

    private final ComplianceRecordRepository repository;
    private final KycEventPublisher kycEventPublisher;

    public Page<ComplianceRecordResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    public Page<ComplianceRecordResponse> findAll(ComplianceRecord.ComplianceStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable).map(this::toResponse);
    }

    public ComplianceRecordResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    public ComplianceRecordResponse findByInvestor(UUID investorId) {
        return repository.findByInvestorId(investorId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ComplianceRecord for investor " + investorId));
    }

    public ComplianceCheckResponse checkWallet(String walletAddress) {
        return repository.findByWalletAddress(walletAddress.toLowerCase())
                .map(record -> new ComplianceCheckResponse(
                        record.getWalletAddress(),
                        record.getStatus() == ComplianceRecord.ComplianceStatus.APPROVED,
                        record.getStatus(),
                        record.getInvestorId(),
                        record.getCountryCode(),
                        record.getKycExpiresAt()))
                .orElseGet(() -> new ComplianceCheckResponse(
                        walletAddress, false, null, null, null, null));
    }

    @Transactional
    public ComplianceRecordResponse register(RegisterComplianceRequest request) {
        if (repository.existsByWalletAddress(request.walletAddress().toLowerCase())) {
            throw new ConflictException("Wallet " + request.walletAddress() + " already registered");
        }
        if (repository.existsByInvestorId(request.investorId())) {
            throw new ConflictException("Investor " + request.investorId() + " already registered");
        }

        ComplianceRecord record = ComplianceRecord.builder()
                .investorId(request.investorId())
                .walletAddress(request.walletAddress().toLowerCase())
                .fullName(request.fullName())
                .countryCode(request.countryCode())
                .kycProvider(request.kycProvider())
                .kycReferenceId(request.kycReferenceId())
                .status(ComplianceRecord.ComplianceStatus.PENDING)
                .build();

        ComplianceRecord saved = repository.save(record);
        log.info("Registered compliance record id={} investor={}", saved.getId(), request.investorId());
        return toResponse(saved);
    }

    @Transactional
    public ComplianceRecordResponse verify(UUID id, ComplianceVerifyRequest request) {
        ComplianceRecord record = getOrThrow(id);
        Instant approvedAt = Instant.now();
        record.setStatus(ComplianceRecord.ComplianceStatus.APPROVED);
        record.setKycVerifiedAt(approvedAt);
        record.setKycExpiresAt(request.kycExpiresAt());

        ComplianceRecord saved = repository.save(record);
        kycEventPublisher.publishKycApproved(
                saved.getInvestorId(),
                saved.getWalletAddress(),
                saved.getCountryCode(),
                saved.getKycExpiresAt(),
                approvedAt);
        log.info("Investor {} KYC approved", saved.getInvestorId());
        return toResponse(saved);
    }

    @Transactional
    public ComplianceRecordResponse revoke(UUID id, String reason) {
        ComplianceRecord record = getOrThrow(id);
        Instant revokedAt = Instant.now();
        record.setStatus(ComplianceRecord.ComplianceStatus.REVOKED);
        record.setRejectionReason(reason);

        ComplianceRecord saved = repository.save(record);
        kycEventPublisher.publishKycRevoked(
                saved.getInvestorId(), saved.getWalletAddress(), reason, revokedAt);
        log.info("Investor {} KYC revoked", saved.getInvestorId());
        return toResponse(saved);
    }

    private ComplianceRecord getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ComplianceRecord", id));
    }

    private ComplianceRecordResponse toResponse(ComplianceRecord record) {
        return new ComplianceRecordResponse(
                record.getId(),
                record.getInvestorId(),
                record.getWalletAddress(),
                record.getFullName(),
                record.getCountryCode(),
                record.getKycProvider(),
                record.getKycVerifiedAt(),
                record.getKycExpiresAt(),
                record.getStatus(),
                record.getRejectionReason(),
                record.getCreatedAt());
    }
}
