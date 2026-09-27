package com.tokenrealty.payment.integration;

import com.tokenrealty.payment.dto.LendingDtos.*;
import com.tokenrealty.payment.entity.LoanAccount;
import com.tokenrealty.payment.service.LendingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Loan liquidation — Phase 16 integration")
class LendingLiquidationIntegrationTest {

    @Autowired LendingService lendingService;

    @Test
    @DisplayName("Admin liquidates active loan and seizes collateral")
    void liquidateLoan() {
        UUID investorId = UUID.randomUUID();
        CollateralPositionResponse collateral = lendingService.depositCollateral(
                DepositCollateralRequest.builder()
                        .investorId(investorId)
                        .contractId(UUID.randomUUID())
                        .tokenAmount(500L)
                        .navPerTokenUsd(new BigDecimal("200.00"))
                        .build());
        LoanAccountResponse loan = lendingService.borrow(BorrowAgainstCollateralRequest.builder()
                .investorId(investorId)
                .collateralPositionId(collateral.id())
                .borrowAmountUsd(new BigDecimal("40000.00"))
                .build());

        LoanAccountResponse liquidated = lendingService.liquidate(loan.id(), LiquidateLoanRequest.builder()
                .adminId(UUID.randomUUID())
                .reason("LTV breach")
                .build());
        assertThat(liquidated.status()).isEqualTo(LoanAccount.LoanStatus.LIQUIDATED);
    }
}
