package com.tokenrealty.payment.repository;

import com.tokenrealty.payment.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, UUID> {

    List<LoanAccount> findByInvestorIdOrderByCreatedAtDesc(UUID investorId);

    List<LoanAccount> findByInvestorIdAndStatus(UUID investorId, LoanAccount.LoanStatus status);
}
