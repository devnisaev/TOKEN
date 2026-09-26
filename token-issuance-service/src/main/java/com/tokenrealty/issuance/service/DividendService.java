package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.entity.DividendPayment;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.issuance.kafka.port.DividendDistributedPublisher;
import com.tokenrealty.issuance.client.RentalClient;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional(readOnly = true)
public class DividendService {

    private final DividendPaymentRepository dividendRepository;
    private final TokenContractRepository contractRepository;
    private final TokenHolderRepository holderRepository;
    private final DividendDistributedPublisher dividendDistributedPublisher;
    private final RentalClient rentalClient;
    private final DividendDistributorService dividendDistributorService;

    public DividendService(DividendPaymentRepository dividendRepository,
                           TokenContractRepository contractRepository,
                           TokenHolderRepository holderRepository,
                           DividendDistributedPublisher dividendDistributedPublisher,
                           RentalClient rentalClient,
                           DividendDistributorService dividendDistributorService) {
        this.dividendRepository = dividendRepository;
        this.contractRepository = contractRepository;
        this.holderRepository = holderRepository;
        this.dividendDistributedPublisher = dividendDistributedPublisher;
        this.rentalClient = rentalClient;
        this.dividendDistributorService = dividendDistributorService;
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
     * Creates DividendPayment records for each holder and publishes
     * dividend.distributed — Payment Service sends on-chain USDC payouts.
     * Future on-chain path: invoke DividendDistributor via PaymentBlockchainService-style
     * Web3j helper using {@code tokenrealty.issuance.dividend-distributor-address} or
     * {@code TokenContract.dividendDistributorAddress}.
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
                    .status(DividendPayment.PaymentStatus.PENDING)
                    .build();

            payments.add(dividendRepository.save(payment));
            totalDistributed = totalDistributed.add(holderShare);
        }

        dividendDistributorService.depositIfConfigured(
                        contract.getDividendDistributorAddress(), totalDistributed)
                .ifPresent(txHash -> log.info(
                        "DividendDistributor deposit tx={} contract={}", txHash, contractId));

        log.info("Scheduled ${} dividend payout to {} holders for contract {} (Payment Service settles on-chain)",
                totalDistributed, payments.size(), contractId);

        String period = request.periodStart().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        dividendDistributedPublisher.publishDividendDistributed(
                new DividendDistributedPublisher.DividendDistributedEvent(
                        contractId,
                        contract.getFlatId(),
                        period,
                        totalDistributed,
                        payments.stream()
                                .map(payment -> new DividendDistributedPublisher.HolderPayout(
                                        payment.getId(),
                                        payment.getInvestorId(),
                                        payment.getInvestorWallet(),
                                        payment.getAmountUsd(),
                                        payment.getOwnershipPct()))
                                .toList(),
                        Instant.now()));

        return new DividendSummaryResponse(
                contractId,
                request.periodStart(),
                request.periodEnd(),
                request.grossRentalIncomeUsd(),
                payments.size(),
                totalDistributed,
                "PENDING_PAYOUT"
        );
    }

    @Transactional
    public void markPaid(UUID dividendPaymentId, String txHash, Instant paidAt) {
        DividendPayment payment = dividendRepository.findById(dividendPaymentId)
                .orElseThrow(() -> new ResourceNotFoundException("DividendPayment", dividendPaymentId));
        if (payment.getStatus() == DividendPayment.PaymentStatus.PAID) {
            log.debug("Dividend payment {} already marked PAID", dividendPaymentId);
            return;
        }
        payment.setTxHash(txHash);
        payment.setPaidAt(paidAt);
        payment.setStatus(DividendPayment.PaymentStatus.PAID);
        dividendRepository.save(payment);
        log.info("Dividend payment {} settled tx={}", dividendPaymentId, txHash);
    }

    /**
     * Scheduled job — runs on the 1st of each month at 9:00 AM.
     * Fetches collected rent from Rental Service for the prior calendar month.
     */
    @Scheduled(cron = "0 0 9 1 * *")
    public void monthlyDistributionJob() {
        log.info("Monthly dividend distribution job started");
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        LocalDate periodStart = lastMonth.withDayOfMonth(1);
        LocalDate periodEnd = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
        String period = periodStart.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        contractRepository.findAll().stream()
                .filter(c -> c.getStatus() == TokenContract.ContractStatus.ACTIVE)
                .forEach(contract -> {
                    try {
                        BigDecimal rentCollected = rentalClient.getRentCollectedForPeriod(
                                contract.getFlatId(), period);
                        if (rentCollected == null || rentCollected.signum() <= 0) {
                            log.info("No rent collected for flat {} period {} — skip dividend",
                                    contract.getFlatId(), period);
                            return;
                        }
                        distribute(contract.getId(), new DistributeDividendRequest(
                                periodStart, periodEnd, rentCollected));
                    } catch (Exception ex) {
                        log.warn("Monthly dividend failed for contract {}: {}",
                                contract.getId(), ex.getMessage());
                    }
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