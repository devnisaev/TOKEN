package com.tokenrealty.compliance.kafka.outbox;

import com.tokenrealty.compliance.kafka.ComplianceKafkaEventTypes;
import com.tokenrealty.compliance.kafka.port.KycEventPublisher;
import com.tokenrealty.outbox.OutboxPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxKycEventPublisher implements KycEventPublisher {

    private final OutboxWriter outboxWriter;

    @Value("${tokenrealty.kafka.topic.kyc-approved:" + ComplianceKafkaEventTypes.KYC_APPROVED + "}")
    private String kycApprovedTopic;

    @Value("${tokenrealty.kafka.topic.kyc-revoked:" + ComplianceKafkaEventTypes.KYC_REVOKED + "}")
    private String kycRevokedTopic;

    @Override
    public void publishKycApproved(UUID investorId, String walletAddress, String countryCode,
                                   Instant kycExpiresAt, Instant approvedAt) {
        OutboxPayload.start()
                .put("investorId", investorId)
                .put("walletAddress", walletAddress)
                .put("countryCode", countryCode != null ? countryCode : "XX")
                .put("kycExpiresAt", kycExpiresAt.toString())
                .put("approvedAt", approvedAt.toString())
                .enqueue(outboxWriter, kycApprovedTopic, investorId.toString());
    }

    @Override
    public void publishKycRevoked(UUID investorId, String walletAddress, String reason, Instant revokedAt) {
        OutboxPayload.start()
                .put("investorId", investorId)
                .put("walletAddress", walletAddress)
                .put("reason", reason)
                .put("revokedAt", revokedAt.toString())
                .enqueue(outboxWriter, kycRevokedTopic, investorId.toString());
    }
}
