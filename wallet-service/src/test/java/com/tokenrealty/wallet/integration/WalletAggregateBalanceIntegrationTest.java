package com.tokenrealty.wallet.integration;

import com.tokenrealty.security.TokenPrincipal;
import com.tokenrealty.security.UserRole;
import com.tokenrealty.wallet.client.IssuanceClient;
import com.tokenrealty.wallet.client.PaymentClient;
import com.tokenrealty.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Wallet aggregate balance integration test")
class WalletAggregateBalanceIntegrationTest {

    @Autowired WalletService walletService;

    @MockitoBean PaymentClient paymentClient;
    @MockitoBean IssuanceClient issuanceClient;

    private UUID investorId;

    @BeforeEach
    void setAdminSecurityContext() {
        investorId = UUID.randomUUID();
        var principal = new TokenPrincipal(investorId, "admin@tokenrealty.com", UserRole.ADMIN, false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    @DisplayName("getAggregateBalance combines payment fiat and token holdings")
    void getAggregateBalance_combinesSources() {
        when(paymentClient.getWalletBalance(investorId))
                .thenReturn(new PaymentClient.WalletBalanceResponse(
                        investorId, "USDC", new BigDecimal("10000.00"), BigDecimal.ZERO));
        when(issuanceClient.getHoldings(investorId)).thenReturn(List.of(
                new IssuanceClient.TokenHolderView(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        investorId,
                        "0x70997970C51812dc3A010C724d1AfE6fc599aa84",
                        50L,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "ACTIVE",
                        null,
                        "SFT-101")));

        var balance = walletService.getAggregateBalance(investorId);

        assertThat(balance.investorId()).isEqualTo(investorId);
        assertThat(balance.fiatBalances()).hasSize(1);
        assertThat(balance.fiatBalances().getFirst().available()).isEqualByComparingTo("10000.00");
        assertThat(balance.tokenHoldings()).hasSize(1);
        assertThat(balance.tokenHoldings().getFirst().tokenSymbol()).isEqualTo("SFT-101");
    }
}
