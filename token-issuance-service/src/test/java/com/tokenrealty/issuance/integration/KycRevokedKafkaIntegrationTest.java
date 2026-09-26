package com.tokenrealty.issuance.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.issuance.blockchain.BlockchainConnector;
import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.command.KycRevokedCommand;
import com.tokenrealty.issuance.service.OnChainWhitelistService;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("KYC revoked Kafka integration test")
class KycRevokedKafkaIntegrationTest {

    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired OnChainWhitelistService onChainWhitelistService;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean BlockchainConnector blockchain;

    @Test
    @DisplayName("kyc-revoked removes wallet from on-chain whitelist")
    void kycRevoked_removesWallet() throws Exception {
        UUID investorId = UUID.randomUUID();
        when(blockchain.sendContractTransaction(any(), any(), eq(BigInteger.ZERO)))
                .thenReturn("0xKycRevokeTx");

        ingestKycRevoked(investorId, "0x70997970C51812dc3A010C724d1AfE6fc599aa84", "expired", UUID.randomUUID());

        verify(blockchain).sendContractTransaction(any(), any(), eq(BigInteger.ZERO));
    }

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void kycRevoked_dedupesDuplicateEventId() throws Exception {
        UUID investorId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(blockchain.sendContractTransaction(any(), any(), eq(BigInteger.ZERO)))
                .thenReturn("0xKycRevokeTx");

        ingestKycRevoked(investorId, "0x70997970C51812dc3A010C724d1AfE6fc599aa84", "expired", eventId);
        ingestKycRevoked(investorId, "0x70997970C51812dc3A010C724d1AfE6fc599aa84", "expired", eventId);

        verify(blockchain).sendContractTransaction(any(), any(), eq(BigInteger.ZERO));
    }

    private void ingestKycRevoked(UUID investorId, String walletAddress, String reason, UUID eventId)
            throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("investorId", investorId.toString());
        payload.put("walletAddress", walletAddress);
        payload.put("reason", reason);

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                IssuanceKafkaEventTypes.KYC_REVOKED,
                investorId.toString(),
                payload));
        eventConsumer.consume(message, IssuanceKafkaEventTypes.KYC_REVOKED,
                "KYC revoked sync failed",
                event -> {
                    KycRevokedCommand command = KycRevokedCommand.from(event);
                    onChainWhitelistService.removeFromWhitelist(command.walletAddress(), command.reason());
                });
    }
}
