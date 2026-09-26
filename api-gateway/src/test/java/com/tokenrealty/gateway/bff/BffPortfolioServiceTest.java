package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.TokenIssuanceClient;
import com.tokenrealty.gateway.client.WalletClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BffPortfolioService unit tests")
class BffPortfolioServiceTest {

    @Mock WalletClient walletClient;
    @Mock TokenIssuanceClient issuanceClient;
    @InjectMocks BffPortfolioService portfolioService;

    @Test
    @DisplayName("getPortfolio aggregates wallet balance and recent dividends")
    void getPortfolio_aggregates() {
        UUID investorId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();
        when(walletClient.getAggregateBalance(investorId)).thenReturn(new WalletClient.AggregateBalanceView(
                investorId,
                "0xabc",
                List.of(new WalletClient.FiatBalanceView("USDC", BigDecimal.TEN, BigDecimal.ZERO)),
                List.of(new WalletClient.TokenHoldingView(contractId, "SUN-101", "0xabc", 100))));
        when(issuanceClient.getContract(contractId)).thenReturn(new TokenIssuanceClient.TokenContractView(
                contractId, flatId, "0xcontract", "SUN-101", 1000L, BigDecimal.ONE, "ACTIVE"));
        when(issuanceClient.listInvestorDividends(investorId)).thenReturn(List.of(
                new TokenIssuanceClient.DividendPaymentView(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        investorId,
                        "0xabc",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        100L,
                        BigDecimal.valueOf(0.1),
                        BigDecimal.valueOf(1000),
                        BigDecimal.valueOf(10),
                        "0xtx",
                        Instant.parse("2026-02-01T00:00:00Z"),
                        "PAID",
                        Instant.parse("2026-02-01T00:00:00Z"))));

        var portfolio = portfolioService.getPortfolio(investorId);

        assertThat(portfolio.balance().tokenHoldings()).hasSize(1);
        assertThat(portfolio.recentDividends()).hasSize(1);
        assertThat(portfolio.recentDividends().getFirst().amountUsd()).isEqualByComparingTo(BigDecimal.TEN);
    }
}
