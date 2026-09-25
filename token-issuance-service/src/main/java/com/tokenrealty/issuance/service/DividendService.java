package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.entity.DividendPayment;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.issuance.exception.ConflictException;
import com.tokenrealty.issuance.exception.ResourceNotFoundException;
import com.tokenrealty.issuance.repository.DividendPaymentRepository;
import com.tokenrealty.issuance.repository.TokenContractRepository;
import com.tokenrealty.issuance.repository.TokenHolderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional(readOnly = true)
public class DividendService {

    // MATIC/USD conversion rate — in production fetch from price oracle
    private static final BigDecimal MATIC_USD_RATE = BigDecimal.valueOf(0.85);

    private final DividendPaymentRepository dividendRepository;
    private final TokenContractRepository contractRepository;
    private final TokenHolderRepository holderRepository;

    public DividendService(DividendPaymentRepository dividendRepository,
                           TokenContractRepository contractRepository,
                           TokenHolderRepository holderRepository) {
        this.dividendRepository = dividendRepository;
        this.contractRepository = contractRepository;
        this.holderRepository = holderRepository;
    }

    public Page<DividendPaymentResponse> findByContract(UUID contractId, Pageable pageable) {
        return dividendRepository.findByTokenContractId(contractId, pageable)
                .map(this::toResponse);
    }

    public List<DividendPaymentResponse> findByInvestor(UUID investorId) {
        return dividendRepository.findByInvestorId(investorId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Distribute rental income to all token holders of a flat.
     * Creates DividendPayment records for each holder and submits
     * on-chain transactions to the DividendDistributor contract.
     */
    @Transactional
    public DividendSummaryResponse distribute(UUID contractId,
                                              DistributeDividendRequest request) {
        TokenContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("TokenContract", contractId));

        if (contract.getStatus() != TokenContract.ContractStatus.ACTIVE) {
            throw new ConflictException("Contract must be ACTIVE for dividend distribution");
        }

        // Prevent duplicate distribution for the same period
        if (dividendRepository.existsByTokenContractIdAndPeriodStartAndPeriodEnd(
                contractId, request.periodStart(), request.periodEnd())) {
            throw new ConflictException("Dividends already distributed for period "
                    + request.periodStart() + " to " + request.periodEnd());
        }

        List<TokenHolder> holders = holderRepository.findByTokenContractId(contractId)
                .stream()
                .filter(h -> h.getBalance() > 0)
                .toList();

        if (holders.isEmpty()) {
            throw new ConflictException("No active token holders found for contract " + contractId);
        }

        log.info("Distributing dividends: contract={} gross=${} period={} to {} holders",
                contractId, request.grossRentalIncomeUsd(),
                request.periodStart(), holders.size());

        List<DividendPayment> payments = new ArrayList<>();
        BigDecimal totalDistributed = BigDecimal.ZERO;

        for (TokenHolder holder : holders) {
            BigDecimal ownershipPct = BigDecimal.valueOf(holder.getBalance())
                    .divide(BigDecimal.valueOf(contract.getTotalSupply()), 8, RoundingMode.HALF_UP);

            BigDecimal holderShare = request.grossRentalIncomeUsd()
                    .multiply(ownershipPct)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal maticAmount = holderShare
                    .divide(MATIC_USD_RATE, 8, RoundingMode.HALF_UP);

            DividendPayment payment = DividendPayment.builder()
                    .tokenContract(contract)
                    .investorId(holder.getInvestorId())
                    .investorWallet(holder.getWalletAddress())
                    .periodStart(request.periodStart())
                    .periodEnd(request.periodEnd())
                    .tokensHeld(holder.getBalance())
                    .ownershipPct(ownershipPct.multiply(BigDecimal.valueOf(100)))
                    .grossRentalIncomeUsd(request.grossRentalIncomeUsd())
                    .amountUsd(holderShare)
                    .amountMatic(maticAmount)
                    .status(DividendPayment.PaymentStatus.PENDING)
                    .build();

            payments.add(dividendRepository.save(payment));
            totalDistributed = totalDistributed.add(holderShare);
        }

        // In production: submit on-chain dividend distribution tx here
        // For now: mark all as PAID (simulated payment)
        for (DividendPayment payment : payments) {
            payment.setStatus(DividendPayment.PaymentStatus.PAID);
            payment.setPaidAt(Instant.now());
            String simId = payment.getId() != null
                    ? payment.getId().toString().replace("-", "").substring(0, 16)
                    : java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            payment.setTxHash("0xSIMULATED_" + simId);
            dividendRepository.save(payment);
        }

        log.info("Distributed ${} to {} holders for contract {}",
                totalDistributed, payments.size(), contractId);

        return new DividendSummaryResponse(
                contractId,
                request.periodStart(),
                request.periodEnd(),
                request.grossRentalIncomeUsd(),
                payments.size(),
                totalDistributed,
                "PAID"
        );
    }

    /**
     * Scheduled job — runs on the 1st of each month at 9:00 AM.
     * In production, this would fetch rental income from a property
     * management integration and trigger distribution automatically.
     */
    @Scheduled(cron = "0 0 9 1 * *")
    public void monthlyDistributionJob() {
        log.info("Monthly dividend distribution job started");
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        LocalDate periodStart = lastMonth.withDayOfMonth(1);
        LocalDate periodEnd = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());

        contractRepository.findAll().stream()
                .filter(c -> c.getStatus() == TokenContract.ContractStatus.ACTIVE)
                .forEach(contract -> {
                    log.info("Processing dividends for contract {} flat={}",
                            contract.getId(), contract.getFlatId());
                    // In production: fetch actual rental income from property management system
                    // For now: skip automatic processing until rental income source is connected
                });
    }

    private DividendPaymentResponse toResponse(DividendPayment d) {
        return new DividendPaymentResponse(
                d.getId(), d.getTokenContract().getId(),
                d.getInvestorId(), d.getInvestorWallet(),
                d.getPeriodStart(), d.getPeriodEnd(),
                d.getTokensHeld(), d.getOwnershipPct(),
                d.getGrossRentalIncomeUsd(), d.getAmountUsd(), d.getAmountMatic(),
                d.getTxHash(), d.getPaidAt(), d.getStatus(), d.getCreatedAt()
        );
    }
}