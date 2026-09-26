package com.tokenrealty.issuance.kafka.in;

import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.command.KycApprovedCommand;
import com.tokenrealty.issuance.service.OnChainWhitelistService;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class KycApprovedListener {

    private final KafkaEventConsumer eventConsumer;
    private final OnChainWhitelistService onChainWhitelistService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.kyc-approved}")
    public void onKycApproved(String message) {
        eventConsumer.consume(message, IssuanceKafkaEventTypes.KYC_APPROVED,
                "KYC approved sync failed",
                event -> {
                    KycApprovedCommand command = KycApprovedCommand.from(event);
                    String txHash = onChainWhitelistService.whitelist(
                            command.walletAddress(),
                            command.countryCode() != null ? command.countryCode() : "XX",
                            command.kycExpiresAt());
                    log.info("On-chain whitelist synced for investor {} tx={}",
                            command.investorId(), txHash);
                });
    }
}
