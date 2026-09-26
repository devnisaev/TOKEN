package com.tokenrealty.rental.repository;

import com.tokenrealty.rental.entity.Lease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LeaseRepository extends JpaRepository<Lease, UUID> {

    Optional<Lease> findFirstByFlatIdAndStatusOrderByStartDateDesc(UUID flatId, Lease.LeaseStatus status);

    boolean existsByFlatIdAndStatus(UUID flatId, Lease.LeaseStatus status);
}
