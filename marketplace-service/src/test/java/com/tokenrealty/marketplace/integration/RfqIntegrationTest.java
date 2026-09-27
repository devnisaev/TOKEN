package com.tokenrealty.marketplace.integration;

import com.tokenrealty.marketplace.client.ComplianceClient;
import com.tokenrealty.marketplace.client.PaymentClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.entity.RfqRequest;
import com.tokenrealty.marketplace.service.RfqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("OTC RFQ — Phase 15 integration")
class RfqIntegrationTest {

    @Autowired RfqService rfqService;

    @MockitoBean ComplianceClient complianceClient;
    @MockitoBean PaymentClient paymentClient;

    UUID buyerId;
    UUID sellerId;
    UUID rfqId;
    UUID quoteId;

    @BeforeEach
    void setUp() {
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        when(complianceClient.checkWallet(any())).thenAnswer(inv -> {
            String wallet = inv.getArgument(0);
            UUID investor = wallet.contains("seller") ? sellerId : buyerId;
            return new ComplianceClient.ComplianceCheckResponse(wallet, true, "APPROVED", investor, "US", null);
        });
        org.mockito.Mockito.doNothing().when(complianceClient).checkInvestment(any(), any(), any());
        when(paymentClient.initiateTokenPurchase(any(), any(), any(), any()))
                .thenAnswer(inv -> new PaymentClient.InitiatePaymentResponse(UUID.randomUUID(), inv.getArgument(0), null));
        when(paymentClient.initiateTokenPurchase(any(), any(), any(), any(), any()))
                .thenAnswer(inv -> new PaymentClient.InitiatePaymentResponse(UUID.randomUUID(), inv.getArgument(0), null));

        RfqRequestResponse rfq = rfqService.createRequest(CreateRfqRequest.builder()
                .contractId(UUID.randomUUID())
                .flatId(UUID.randomUUID())
                .buildingId(UUID.randomUUID())
                .side(RfqRequest.RfqSide.BUY)
                .tokenAmount(5000L)
                .indicativePriceUsd(new BigDecimal("100.00"))
                .requesterId(buyerId)
                .walletAddress("0xbuyer")
                .liquidityTier("TIER_2")
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build());
        rfqId = rfq.id();

        RfqQuoteResponse quote = rfqService.submitQuote(rfqId, SubmitRfqQuoteRequest.builder()
                .quoterId(sellerId)
                .walletAddress("0xseller")
                .pricePerTokenUsd(new BigDecimal("100.00"))
                .build());
        quoteId = quote.id();
    }

    @Test
    @DisplayName("Accept RFQ quote initiates settlement")
    void acceptQuote() {
        RfqRequestResponse accepted = rfqService.acceptQuote(rfqId, AcceptRfqQuoteRequest.builder()
                .requesterId(buyerId)
                .quoteId(quoteId)
                .build());
        assertThat(accepted.status()).isEqualTo(RfqRequest.RfqStatus.ACCEPTED);
        assertThat(accepted.acceptedQuoteId()).isEqualTo(quoteId);
    }
}
