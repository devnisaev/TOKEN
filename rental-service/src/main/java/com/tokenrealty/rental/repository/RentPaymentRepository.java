package com.tokenrealty.rental.repository;

import com.tokenrealty.rental.entity.RentPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RentPaymentRepository extends JpaRepository<RentPayment, UUID> {

    boolean existsByLeaseIdAndPeriod(UUID leaseId, String period);

    List<RentPayment> findByLeaseIdOrderByPeriodDesc(UUID leaseId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM RentPayment r "
            + "WHERE r.flatId = :flatId AND r.period = :period")
    BigDecimal sumAmountByFlatIdAndPeriod(@Param("flatId") UUID flatId, @Param("period") String period);
}
