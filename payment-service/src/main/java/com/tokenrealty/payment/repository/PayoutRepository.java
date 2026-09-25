package com.tokenrealty.payment.repository;

import com.tokenrealty.payment.entity.Payout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    Page<Payout> findByRecipientInvestorId(UUID recipientInvestorId, Pageable pageable);
}
