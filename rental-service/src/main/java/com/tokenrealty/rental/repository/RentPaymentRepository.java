package com.tokenrealty.rental.repository;

import com.tokenrealty.rental.entity.RentPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RentPaymentRepository extends JpaRepository<RentPayment, UUID> {

    boolean existsByLeaseIdAndPeriod(UUID leaseId, String period);
}
