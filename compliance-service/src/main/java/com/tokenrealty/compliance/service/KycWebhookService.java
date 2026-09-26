package com.tokenrealty.compliance.service;

import com.tokenrealty.compliance.dto.ComplianceDtos.ComplianceVerifyRequest;
import com.tokenrealty.compliance.dto.ComplianceDtos.KycWebhookPayload;
import com.tokenrealty.compliance.entity.ComplianceRecord;
import com.tokenrealty.compliance.repository.ComplianceRecordRepository;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KycWebhookService {

    private static final String SUMSUB_PROVIDER = "sumsub";

    private final ComplianceRecordRepository repository;
    private final ComplianceService complianceService;

    @Transactional
    public void handleWebhook(String provider, KycWebhookPayload payload) {
        if (!SUMSUB_PROVIDER.equalsIgnoreCase(provider)) {
            raiseValidation("Unsupported KYC provider: " + provider);
        }
        ComplianceRecord record = resolveRecord(payload);
        String reviewAnswer = payload.reviewResult() != null
                ? payload.reviewResult().reviewAnswer()
                : null;

        if ("completed".equalsIgnoreCase(payload.reviewStatus()) && "GREEN".equalsIgnoreCase(reviewAnswer)) {
            Instant expiresAt = payload.kycExpiresAt() != null
                    ? payload.kycExpiresAt()
                    : Instant.now().plusSeconds(365L * 24 * 60 * 60);
            complianceService.verify(record.getId(), new ComplianceVerifyRequest(expiresAt));
            log.info("KYC webhook approved investor={} provider={}", record.getInvestorId(), provider);
            return;
        }

        if ("completed".equalsIgnoreCase(payload.reviewStatus()) && "RED".equalsIgnoreCase(reviewAnswer)) {
            String reason = payload.reviewResult() != null && payload.reviewResult().rejectLabels() != null
                    ? String.join(", ", payload.reviewResult().rejectLabels())
                    : "KYC rejected by provider";
            complianceService.revoke(record.getId(), reason);
            log.info("KYC webhook revoked investor={} provider={}", record.getInvestorId(), provider);
            return;
        }

        log.debug("Ignoring KYC webhook status={} answer={} for investor={}",
                payload.reviewStatus(), reviewAnswer, record.getInvestorId());
    }

    private ComplianceRecord resolveRecord(KycWebhookPayload payload) {
        if (payload.externalUserId() != null) {
            UUID investorId = UUID.fromString(payload.externalUserId());
            return repository.findByInvestorId(investorId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ComplianceRecord for investor " + investorId));
        }
        if (payload.applicantId() != null) {
            return repository.findByKycReferenceId(payload.applicantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ComplianceRecord for applicant " + payload.applicantId()));
        }
        raiseValidation("Webhook payload must include externalUserId or applicantId");
        return null;
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
