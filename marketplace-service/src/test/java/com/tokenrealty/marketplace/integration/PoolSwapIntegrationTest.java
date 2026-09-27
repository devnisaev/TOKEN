package com.tokenrealty.marketplace.integration;

import com.tokenrealty.marketplace.client.ComplianceClient;
import com.tokenrealty.marketplace.client.PaymentClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.entity.LiquidityPool;
import com.tokenrealty.marketplace.entity.PoolSwap;
import com.tokenrealty.marketplace.repository.LiquidityPoolRepository;
import com.tokenrealty.marketplace.service.LiquidityPoolService;
import com.tokenrealty.marketplace.service.NavCircuitBreakerService;
import com.tokenrealty.marketplace.service.PoolSwapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AMM pool swap — Phase 14 integration")
class PoolSwapIntegrationTest {

    @Autowired LiquidityPoolService liquidityPoolService;
    @Autowired PoolSwapService poolSwapService;
    @Autowired NavCircuitBreakerService navCircuitBreakerService;
    @Autowired LiquidityPoolRepository liquidityPoolRepository;

    @MockitoBean ComplianceClient complianceClient;
    @MockitoBean PaymentClient paymentClient;

    UUID contractId;
    UUID flatId;
    UUID buildingId;
    UUID investorId;
    UUID poolId;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        flatId = UUID.randomUUID();
        buildingId = UUID.randomUUID();
        investorId = UUID.randomUUID();

        when(complianceClient.checkWallet(any())).thenReturn(
                new ComplianceClient.ComplianceCheckResponse("0xinv", true, "APPROVED", investorId, "US", null));
        org.mockito.Mockito.doNothing().when(complianceClient).checkInvestment(any(), any(), any());
        var paymentResponse = new PaymentClient.InitiatePaymentResponse(UUID.randomUUID(), UUID.randomUUID(), null);
        when(paymentClient.initiateTokenPurchase(any(), any(), any(), any()))
                .thenAnswer(inv -> new PaymentClient.InitiatePaymentResponse(UUID.randomUUID(), inv.getArgument(0), null));
        when(paymentClient.initiateTokenPurchase(any(), any(), any(), any(), any()))
                .thenReturn(paymentResponse);

        LiquidityPoolResponse pool = liquidityPoolService.createPool(CreateLiquidityPoolRequest.builder()
                .contractId(contractId)
                .flatId(flatId)
                .buildingId(buildingId)
                .feeBps(30)
                .navBreakPct(new BigDecimal("10"))
                .liquidityTier("TIER_1")
                .build());
        poolId = pool.id();
        liquidityPoolService.seedPool(poolId, SeedLiquidityPoolRequest.builder()
                .tokenAmount(1000L)
                .usdcAmount(new BigDecimal("100000"))
                .investorId(investorId)
                .build());
    }

    @Test
    @DisplayName("Swap USDC for tokens updates reserves")
    void swapUsdcForTokens() {
        SwapQuoteResponse quote = poolSwapService.quote(poolId, new SwapQuoteRequest(
                PoolSwap.SwapDirection.USDC_TO_TOKEN, new BigDecimal("1000.00")));
        assertThat(quote.amountOut().longValue()).isGreaterThan(0L);

        PoolSwapResponse swap = poolSwapService.executeSwap(poolId, ExecuteSwapRequest.builder()
                .direction(PoolSwap.SwapDirection.USDC_TO_TOKEN)
                .amountIn(new BigDecimal("1000.00"))
                .investorId(investorId)
                .walletAddress("0xinv")
                .build());
        assertThat(swap.status()).isEqualTo(PoolSwap.SwapStatus.COMPLETED);

        LiquidityPool pool = liquidityPoolRepository.findById(poolId).orElseThrow();
        assertThat(pool.getUsdcReserve()).isGreaterThan(new BigDecimal("100000"));
        assertThat(pool.getTokenReserve()).isLessThan(1000L);
    }

    @Test
    @DisplayName("NAV circuit breaker pauses pool on large divergence")
    void navCircuitBreakerPausesPool() {
        navCircuitBreakerService.evaluatePool(
                liquidityPoolRepository.findById(poolId).orElseThrow(),
                new BigDecimal("50.00"),
                Instant.now());
        LiquidityPool pool = liquidityPoolRepository.findById(poolId).orElseThrow();
        assertThat(pool.getStatus()).isEqualTo(LiquidityPool.PoolStatus.PAUSED);
    }
}
