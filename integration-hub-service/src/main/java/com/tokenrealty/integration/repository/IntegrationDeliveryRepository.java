package com.tokenrealty.integration.repository;

import com.tokenrealty.integration.entity.IntegrationDelivery;
import com.tokenrealty.integration.entity.IntegrationDeliveryStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IntegrationDeliveryRepository extends JpaRepository<IntegrationDelivery, UUID> {

    @Query("""
            SELECT d FROM IntegrationDelivery d
            WHERE d.status IN :statuses
              AND d.attempts < :maxAttempts
              AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now)
            ORDER BY d.createdAt ASC
            """)
    List<IntegrationDelivery> findRetryCandidates(
            @Param("statuses") List<IntegrationDeliveryStatus> statuses,
            @Param("now") Instant now,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable);
}
