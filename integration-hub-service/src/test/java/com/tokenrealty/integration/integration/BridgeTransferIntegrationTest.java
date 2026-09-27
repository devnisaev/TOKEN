package com.tokenrealty.integration.integration;

import com.tokenrealty.integration.dto.IntegrationDtos.*;
import com.tokenrealty.integration.entity.BridgeTransfer;
import com.tokenrealty.integration.service.BridgeTransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Cross-chain bridge — Phase 16 integration")
class BridgeTransferIntegrationTest {

    @Autowired BridgeTransferService bridgeTransferService;

    @Test
    @DisplayName("Create and relay bridge transfer")
    void createAndRelay() {
        BridgeTransferResponse created = bridgeTransferService.create(CreateBridgeTransferRequest.builder()
                .sourceContractId(UUID.randomUUID())
                .sourceChain("polygon")
                .targetChain("arbitrum")
                .investorId(UUID.randomUUID())
                .walletAddress("0xinv")
                .tokenAmount(100L)
                .build());
        assertThat(created.status()).isEqualTo(BridgeTransfer.BridgeStatus.PENDING);

        BridgeTransferResponse relayed = bridgeTransferService.relay(created.id());
        assertThat(relayed.status()).isEqualTo(BridgeTransfer.BridgeStatus.RELAYED);
        assertThat(relayed.relayTxHash()).isNotBlank();
    }
}
