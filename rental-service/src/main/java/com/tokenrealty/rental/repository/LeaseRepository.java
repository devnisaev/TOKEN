package com.tokenrealty.rental.repository;

import com.tokenrealty.rental.entity.Lease;
import com.tokenrealty.rental.entity.Lease.LeaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaseRepository extends JpaRepository<Lease, UUID> {

    Optional<Lease> findFirstByFlatIdAndStatusOrderByStartDateDesc(UUID flatId, LeaseStatus status);

    boolean existsByFlatIdAndStatus(UUID flatId, LeaseStatus status);

    List<Lease> findByStatus(LeaseStatus status);

    List<Lease> findByStatusAndEndDateBefore(LeaseStatus status, LocalDate endDate);
}
