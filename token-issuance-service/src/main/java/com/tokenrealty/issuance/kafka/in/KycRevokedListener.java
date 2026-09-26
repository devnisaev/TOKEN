package com.tokenrealty.issuance.kafka.in;

import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.command.KycRevokedCommand;
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
public class KycRevokedListener {

    private final KafkaEventConsumer eventConsumer;
    private final OnChainWhitelistService onChainWhitelistService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.kyc-revoked}")
    public void onKycRevoked(String message) {
        eventConsumer.consume(message, IssuanceKafkaEventTypes.KYC_REVOKED,
                "KYC revoked sync failed",
                event -> {
                    KycRevokedCommand command = KycRevokedCommand.from(event);
                    onChainWhitelistService.removeFromWhitelist(command.walletAddress(), command.reason());
                    log.info("On-chain whitelist removed for investor {}", command.investorId());
                });
    }
}
