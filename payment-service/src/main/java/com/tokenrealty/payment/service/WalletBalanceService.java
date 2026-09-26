package com.tokenrealty.payment.service;

import com.tokenrealty.payment.dto.PaymentDtos.WalletBalanceResponse;
import com.tokenrealty.payment.entity.PaymentCurrency;
import com.tokenrealty.payment.entity.WalletBalance;
import com.tokenrealty.payment.mapper.PaymentMapper;
import com.tokenrealty.payment.repository.WalletBalanceRepository;
import com.tokenrealty.web.exception.InsufficientFundsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletBalanceService {

    private final WalletBalanceRepository repository;
    private final PaymentMapper mapper;

    public WalletBalanceResponse getByInvestorId(UUID investorId) {
        return repository.findByInvestorIdAndCurrency(investorId, PaymentCurrency.USDC)
                .map(mapper::toWalletBalanceResponse)
                .orElseGet(() -> WalletBalanceResponse.builder()
                        .investorId(investorId)
                        .currency(PaymentCurrency.USDC)
                        .availableBalance(BigDecimal.ZERO)
                        .heldBalance(BigDecimal.ZERO)
                        .build());
    }

    @Transactional
    public WalletBalance ensureBalanceRow(UUID investorId, PaymentCurrency currency) {
        return repository.findByInvestorIdAndCurrency(investorId, currency)
                .orElseGet(() -> repository.save(WalletBalance.builder()
                        .investorId(investorId)
                        .currency(currency)
                        .availableBalance(BigDecimal.ZERO)
                        .heldBalance(BigDecimal.ZERO)
                        .build()));
    }

    /** Custodial path: move available → held when escrow is initiated. */
    @Transactional
    public void holdForPayment(UUID investorId, BigDecimal amount, PaymentCurrency currency) {
        var existing = repository.findByInvestorIdAndCurrency(investorId, currency);
        if (existing.isEmpty()) {
            return;
        }
        WalletBalance balance = existing.get();
        if (balance.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient USDC balance");
        }
        balance.setAvailableBalance(balance.getAvailableBalance().subtract(amount));
        balance.setHeldBalance(balance.getHeldBalance().add(amount));
        repository.save(balance);
    }

    /** On confirm: release held funds into platform escrow (decrease held). */
    @Transactional
    public void settleConfirmedPayment(UUID investorId, BigDecimal amount, PaymentCurrency currency) {
        WalletBalance balance = repository.findByInvestorIdAndCurrency(investorId, currency).orElse(null);
        if (balance == null) {
            return;
        }
        BigDecimal fromHeld = balance.getHeldBalance().min(amount);
        balance.setHeldBalance(balance.getHeldBalance().subtract(fromHeld));
        BigDecimal remainder = amount.subtract(fromHeld);
        if (remainder.compareTo(BigDecimal.ZERO) > 0) {
            balance.setAvailableBalance(balance.getAvailableBalance().subtract(remainder).max(BigDecimal.ZERO));
        }
        repository.save(balance);
    }

    @Transactional
    public void releaseHold(UUID investorId, BigDecimal amount, PaymentCurrency currency) {
        WalletBalance balance = repository.findByInvestorIdAndCurrency(investorId, currency).orElse(null);
        if (balance == null) {
            return;
        }
        BigDecimal fromHeld = balance.getHeldBalance().min(amount);
        balance.setHeldBalance(balance.getHeldBalance().subtract(fromHeld));
        balance.setAvailableBalance(balance.getAvailableBalance().add(fromHeld));
        repository.save(balance);
    }

    @Transactional
    public void credit(UUID investorId, BigDecimal amount, PaymentCurrency currency) {
        WalletBalance balance = ensureBalanceRow(investorId, currency);
        balance.setAvailableBalance(balance.getAvailableBalance().add(amount));
        repository.save(balance);
    }

    @Transactional
    public void debitIfSufficient(UUID investorId, BigDecimal amount, PaymentCurrency currency) {
        WalletBalance balance = ensureBalanceRow(investorId, currency);
        if (balance.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient USDC balance");
        }
        balance.setAvailableBalance(balance.getAvailableBalance().subtract(amount));
        repository.save(balance);
    }
}
