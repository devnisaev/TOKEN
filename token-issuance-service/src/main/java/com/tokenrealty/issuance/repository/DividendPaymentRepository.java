package com.tokenrealty.issuance.repository;

import com.tokenrealty.issuance.entity.DividendPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DividendPaymentRepository extends JpaRepository<DividendPayment, UUID> {

    Page<DividendPayment> findByTokenContractId(UUID contractId, Pageable pageable);

    List<DividendPayment> findByInvestorId(UUID investorId);

    List<DividendPayment> findByTokenContractIdAndStatus(
            UUID contractId, DividendPayment.PaymentStatus status);

    boolean existsByTokenContractIdAndPeriodStartAndPeriodEnd(
            UUID contractId, LocalDate periodStart, LocalDate periodEnd);

    @Query("SELECT SUM(d.amountUsd) FROM DividendPayment d " +
            "WHERE d.tokenContract.id = :contractId AND d.status = 'PAID'")
    java.math.BigDecimal sumPaidDividends(@Param("contractId") UUID contractId);
}