package com.tokenrealty.payment.service;

import com.tokenrealty.payment.dto.PaymentDtos.WalletBalanceResponse;
import com.tokenrealty.payment.entity.PaymentCurrency;
import com.tokenrealty.payment.entity.WalletBalance;
import com.tokenrealty.payment.mapper.PaymentMapper;
import com.tokenrealty.payment.repository.WalletBalanceRepository;
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
    public WalletBalance ensureBalanceRow(UUID investorId) {
        return repository.findByInvestorIdAndCurrency(investorId, PaymentCurrency.USDC)
                .orElseGet(() -> repository.save(WalletBalance.builder()
                        .investorId(investorId)
                        .currency(PaymentCurrency.USDC)
                        .availableBalance(BigDecimal.ZERO)
                        .heldBalance(BigDecimal.ZERO)
                        .build()));
    }
}
