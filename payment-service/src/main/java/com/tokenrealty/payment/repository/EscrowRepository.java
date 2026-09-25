package com.tokenrealty.payment.repository;

import com.tokenrealty.payment.entity.Escrow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EscrowRepository extends JpaRepository<Escrow, UUID> {

    Optional<Escrow> findByPaymentId(UUID paymentId);
}
