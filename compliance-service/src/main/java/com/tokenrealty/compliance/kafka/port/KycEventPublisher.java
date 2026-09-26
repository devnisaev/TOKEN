package com.tokenrealty.compliance.kafka.port;

import java.time.Instant;
import java.util.UUID;

public interface KycEventPublisher {

    void publishKycApproved(UUID investorId, String walletAddress, String countryCode,
                            Instant kycExpiresAt, Instant approvedAt);

    void publishKycRevoked(UUID investorId, String walletAddress, String reason, Instant revokedAt);
}
