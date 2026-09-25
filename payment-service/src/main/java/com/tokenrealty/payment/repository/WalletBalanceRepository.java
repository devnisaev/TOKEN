package com.tokenrealty.payment.repository;

import com.tokenrealty.payment.entity.PaymentCurrency;
import com.tokenrealty.payment.entity.WalletBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletBalanceRepository extends JpaRepository<WalletBalance, UUID> {

    Optional<WalletBalance> findByInvestorIdAndCurrency(UUID investorId, PaymentCurrency currency);
}
