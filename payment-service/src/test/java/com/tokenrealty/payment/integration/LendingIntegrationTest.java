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
@DisplayName("Token collateral lending — Phase 15 integration")
class LendingIntegrationTest {

    @Autowired LendingService lendingService;

    @Test
    @DisplayName("Deposit collateral, borrow within LTV, repay loan")
    void borrowAndRepay() {
        UUID investorId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        CollateralPositionResponse collateral = lendingService.depositCollateral(
                DepositCollateralRequest.builder()
                        .investorId(investorId)
                        .contractId(contractId)
                        .tokenAmount(1000L)
                        .navPerTokenUsd(new BigDecimal("100.00"))
                        .liquidityTier("TIER_1")
                        .build());

        LoanAccountResponse loan = lendingService.borrow(BorrowAgainstCollateralRequest.builder()
                .investorId(investorId)
                .collateralPositionId(collateral.id())
                .borrowAmountUsd(new BigDecimal("50000.00"))
                .build());
        assertThat(loan.outstandingUsd()).isEqualByComparingTo("50000.00");

        LoanAccountResponse repaid = lendingService.repay(loan.id(), RepayLoanRequest.builder()
                .investorId(investorId)
                .principalUsd(new BigDecimal("50000.00"))
                .interestUsd(BigDecimal.ZERO)
                .build());
        assertThat(repaid.status()).isEqualTo(LoanAccount.LoanStatus.REPAID);
    }
}
