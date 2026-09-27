package com.tokenrealty.payment.service;

import com.tokenrealty.payment.dto.LendingDtos.*;
import com.tokenrealty.payment.entity.CollateralPosition;
import com.tokenrealty.payment.entity.LoanAccount;
import com.tokenrealty.payment.entity.PaymentCurrency;
import com.tokenrealty.payment.repository.CollateralPositionRepository;
import com.tokenrealty.payment.repository.LoanAccountRepository;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LendingService {

    private final CollateralPositionRepository collateralPositionRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final LedgerService ledgerService;

    public LendingDashboardResponse getDashboard(UUID investorId) {
        List<CollateralPosition> collateral = collateralPositionRepository.findByInvestorIdAndStatus(
                investorId, CollateralPosition.CollateralStatus.LOCKED);
        List<LoanAccount> loans = loanAccountRepository.findByInvestorIdAndStatus(
                investorId, LoanAccount.LoanStatus.ACTIVE);

        BigDecimal totalCollateral = collateral.stream()
                .map(c -> c.getNavPerTokenUsd().multiply(BigDecimal.valueOf(c.getTokenAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOutstanding = loans.stream()
                .map(LoanAccount::getOutstandingUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maxLtv = collateral.isEmpty()
                ? BigDecimal.ZERO
                : maxLtvPct(collateral.getFirst().getLiquidityTier());
        BigDecimal availableBorrow = totalCollateral.multiply(maxLtv)
                .subtract(totalOutstanding)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.DOWN);

        return LendingDashboardResponse.builder()
                .investorId(investorId)
                .totalCollateralUsd(totalCollateral.setScale(2, RoundingMode.HALF_UP))
                .totalOutstandingUsd(totalOutstanding)
                .availableBorrowUsd(availableBorrow)
                .collateral(collateral.stream().map(this::toCollateralResponse).toList())
                .loans(loans.stream().map(this::toLoanResponse).toList())
                .build();
    }

    @Transactional
    public CollateralPositionResponse depositCollateral(DepositCollateralRequest request) {
        CollateralPosition position = collateralPositionRepository.save(CollateralPosition.builder()
                .investorId(request.investorId())
                .contractId(request.contractId())
                .tokenAmount(request.tokenAmount())
                .navPerTokenUsd(request.navPerTokenUsd())
                .liquidityTier(request.liquidityTier() != null ? request.liquidityTier() : "TIER_1")
                .status(CollateralPosition.CollateralStatus.LOCKED)
                .build());
        return toCollateralResponse(position);
    }

    @Transactional
    public LoanAccountResponse borrow(BorrowAgainstCollateralRequest request) {
        CollateralPosition collateral = collateralPositionRepository.findById(request.collateralPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("CollateralPosition", request.collateralPositionId()));
        if (!collateral.getInvestorId().equals(request.investorId())) {
            raiseValidation("Collateral does not belong to investor");
        }
        if (collateral.getStatus() != CollateralPosition.CollateralStatus.LOCKED) {
            raiseValidation("Collateral is not available for borrowing");
        }

        BigDecimal collateralValue = collateral.getNavPerTokenUsd()
                .multiply(BigDecimal.valueOf(collateral.getTokenAmount()));
        BigDecimal maxBorrow = collateralValue.multiply(maxLtvPct(collateral.getLiquidityTier()))
                .setScale(2, RoundingMode.DOWN);
        if (request.borrowAmountUsd().compareTo(maxBorrow) > 0) {
            raiseValidation("Borrow amount exceeds max LTV (" + maxBorrow + " USD)");
        }

        LoanAccount loan = loanAccountRepository.save(LoanAccount.builder()
                .investorId(request.investorId())
                .collateralPositionId(collateral.getId())
                .principalUsd(request.borrowAmountUsd())
                .outstandingUsd(request.borrowAmountUsd())
                .build());

        ledgerService.recordBorrow(loan.getId(), request.borrowAmountUsd(), PaymentCurrency.USDC);
        return toLoanResponse(loan);
    }

    @Transactional
    public LoanAccountResponse repay(UUID loanId, RepayLoanRequest request) {
        LoanAccount loan = loanAccountRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("LoanAccount", loanId));
        if (!loan.getInvestorId().equals(request.investorId())) {
            raiseValidation("Loan does not belong to investor");
        }
        if (loan.getStatus() != LoanAccount.LoanStatus.ACTIVE) {
            raiseValidation("Loan is not active");
        }
        BigDecimal interest = request.interestUsd() != null ? request.interestUsd() : BigDecimal.ZERO;
        BigDecimal total = request.principalUsd().add(interest);
        if (total.compareTo(loan.getOutstandingUsd()) > 0) {
            raiseValidation("Repayment exceeds outstanding balance");
        }

        ledgerService.recordRepay(loan.getId(), request.principalUsd(), interest, PaymentCurrency.USDC);
        loan.setOutstandingUsd(loan.getOutstandingUsd().subtract(total));
        if (loan.getOutstandingUsd().signum() <= 0) {
            loan.setStatus(LoanAccount.LoanStatus.REPAID);
            loan.setOutstandingUsd(BigDecimal.ZERO);
            releaseCollateral(loan.getCollateralPositionId());
        }
        return toLoanResponse(loanAccountRepository.save(loan));
    }

    @Transactional
    public LoanAccountResponse liquidate(UUID loanId, LiquidateLoanRequest request) {
        LoanAccount loan = loanAccountRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("LoanAccount", loanId));
        if (loan.getStatus() != LoanAccount.LoanStatus.ACTIVE) {
            raiseValidation("Only active loans can be liquidated");
        }
        CollateralPosition collateral = collateralPositionRepository.findById(loan.getCollateralPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("CollateralPosition", loan.getCollateralPositionId()));
        BigDecimal seizedValue = collateral.getNavPerTokenUsd()
                .multiply(BigDecimal.valueOf(collateral.getTokenAmount()))
                .setScale(2, RoundingMode.HALF_UP);

        ledgerService.recordLiquidation(loan.getId(), seizedValue, PaymentCurrency.USDC);
        loan.setStatus(LoanAccount.LoanStatus.LIQUIDATED);
        loan.setOutstandingUsd(BigDecimal.ZERO);
        loanAccountRepository.save(loan);

        collateral.setStatus(CollateralPosition.CollateralStatus.LIQUIDATED);
        collateralPositionRepository.save(collateral);
        return toLoanResponse(loan);
    }

    private void releaseCollateral(UUID collateralPositionId) {
        collateralPositionRepository.findById(collateralPositionId).ifPresent(c -> {
            c.setStatus(CollateralPosition.CollateralStatus.RELEASED);
            collateralPositionRepository.save(c);
        });
    }

    private static BigDecimal maxLtvPct(String tier) {
        return "TIER_2".equals(tier) ? new BigDecimal("0.50") : new BigDecimal("0.65");
    }

    private CollateralPositionResponse toCollateralResponse(CollateralPosition c) {
        return CollateralPositionResponse.builder()
                .id(c.getId())
                .investorId(c.getInvestorId())
                .contractId(c.getContractId())
                .tokenAmount(c.getTokenAmount())
                .navPerTokenUsd(c.getNavPerTokenUsd())
                .collateralValueUsd(c.getNavPerTokenUsd()
                        .multiply(BigDecimal.valueOf(c.getTokenAmount()))
                        .setScale(2, RoundingMode.HALF_UP))
                .liquidityTier(c.getLiquidityTier())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private LoanAccountResponse toLoanResponse(LoanAccount loan) {
        return LoanAccountResponse.builder()
                .id(loan.getId())
                .investorId(loan.getInvestorId())
                .collateralPositionId(loan.getCollateralPositionId())
                .principalUsd(loan.getPrincipalUsd())
                .outstandingUsd(loan.getOutstandingUsd())
                .interestRateBps(loan.getInterestRateBps())
                .status(loan.getStatus())
                .createdAt(loan.getCreatedAt())
                .build();
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
