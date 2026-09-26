package com.tokenrealty.compliance.service;

import com.tokenrealty.compliance.dto.ComplianceDtos.InvestmentCheckResponse;
import com.tokenrealty.compliance.entity.ComplianceRecord;
import com.tokenrealty.compliance.entity.InvestmentPolicy;
import com.tokenrealty.compliance.repository.ComplianceRecordRepository;
import com.tokenrealty.compliance.repository.InvestmentPolicyRepository;
import com.tokenrealty.web.exception.ComplianceBlockedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvestmentPolicyService {

    private final InvestmentPolicyRepository policyRepository;
    private final ComplianceRecordRepository complianceRecordRepository;

    public InvestmentCheckResponse checkInvestment(UUID investorId, String countryCode, BigDecimal amountUsd) {
        String jurisdiction = normalizeCountry(countryCode);
        InvestmentPolicy policy = policyRepository.findByJurisdiction(jurisdiction).orElse(null);
        if (policy == null) {
            return new InvestmentCheckResponse(investorId, jurisdiction, amountUsd, true, null);
        }

        validateAmount(policy, amountUsd);
        if (policy.isAccreditedOnly()) {
            validateAccredited(investorId);
        }

        return new InvestmentCheckResponse(
                investorId,
                jurisdiction,
                amountUsd,
                true,
                policy.getMinInvestmentUsd());
    }

    private void validateAmount(InvestmentPolicy policy, BigDecimal amountUsd) {
        if (amountUsd.compareTo(policy.getMinInvestmentUsd()) < 0) {
            raiseBlocked("Investment below minimum for jurisdiction "
                    + policy.getJurisdiction() + ": " + policy.getMinInvestmentUsd());
        }
        if (policy.getMaxInvestmentUsd() != null
                && amountUsd.compareTo(policy.getMaxInvestmentUsd()) > 0) {
            raiseBlocked("Investment exceeds maximum for jurisdiction "
                    + policy.getJurisdiction() + ": " + policy.getMaxInvestmentUsd());
        }
    }

    private void validateAccredited(UUID investorId) {
        ComplianceRecord record = complianceRecordRepository.findByInvestorId(investorId)
                .orElseThrow(() -> new ComplianceBlockedException(
                        "Accredited investors only — no compliance record for " + investorId));
        if (record.getStatus() != ComplianceRecord.ComplianceStatus.APPROVED) {
            raiseBlocked("Accredited investors only — KYC not approved for " + investorId);
        }
    }

    private static String normalizeCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            raiseBlocked("Country code is required for investment policy check");
        }
        return countryCode.trim().toUpperCase();
    }

    private static void raiseBlocked(String message) {
        throw new ComplianceBlockedException(message);
    }
}
