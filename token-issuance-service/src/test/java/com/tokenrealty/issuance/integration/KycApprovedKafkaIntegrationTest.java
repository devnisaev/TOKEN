package com.tokenrealty.issuance.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.issuance.blockchain.BlockchainConnector;
import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.command.KycApprovedCommand;
import com.tokenrealty.issuance.service.OnChainWhitelistService;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigInteger;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("KYC approved Kafka integration test")
class KycApprovedKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired OnChainWhitelistService onChainWhitelistService;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean BlockchainConnector blockchain;

    @Test
    @DisplayName("kyc-approved whitelists wallet on-chain")
    void kycApproved_whitelistsWallet() throws Exception {
        UUID investorId = UUID.randomUUID();
        String wallet = "0x70997970C51812dc3A010C724d1AfE6fc599aa84";
        when(blockchain.sendContractTransaction(any(), any(), eq(BigInteger.ZERO)))
                .thenReturn("0xKycWhitelistTx");

        ingestKycApproved(investorId, wallet, UUID.randomUUID());

        verify(blockchain).sendContractTransaction(any(), any(), eq(BigInteger.ZERO));
    }

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void kycApproved_dedupesDuplicateEventId() throws Exception {
        UUID investorId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(blockchain.sendContractTransaction(any(), any(), eq(BigInteger.ZERO)))
                .thenReturn("0xKycWhitelistTx");

        ingestKycApproved(investorId, "0x70997970C51812dc3A010C724d1AfE6fc599aa84", eventId);
        ingestKycApproved(investorId, "0x70997970C51812dc3A010C724d1AfE6fc599aa84", eventId);

        verify(blockchain).sendContractTransaction(any(), any(), eq(BigInteger.ZERO));
    }

    private void ingestKycApproved(UUID investorId, String walletAddress, UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("investorId", investorId.toString());
        payload.put("walletAddress", walletAddress);
        payload.put("countryCode", "US");
        payload.put("kycExpiresAt", Instant.now().plusSeconds(86400).toString());

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                IssuanceKafkaEventTypes.KYC_APPROVED,
                investorId.toString(),
                payload));
        eventConsumer.consume(message, IssuanceKafkaEventTypes.KYC_APPROVED,
                "KYC approved sync failed",
                event -> {
                    KycApprovedCommand command = KycApprovedCommand.from(event);
                    onChainWhitelistService.whitelist(
                            command.walletAddress(),
                            command.countryCode() != null ? command.countryCode() : "XX",
                            command.kycExpiresAt());
                });
    }
}
