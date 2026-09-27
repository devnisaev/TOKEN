package com.tokenrealty.marketplace.repository;

import com.tokenrealty.marketplace.entity.ExchangeFill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeFillRepository extends JpaRepository<ExchangeFill, UUID> {

    Page<ExchangeFill> findByContractIdOrderByExecutedAtDesc(UUID contractId, Pageable pageable);

    Optional<ExchangeFill> findTopByContractIdOrderByExecutedAtDesc(UUID contractId);

    @Query("""
            SELECT COALESCE(SUM(f.tokenAmount), 0) FROM ExchangeFill f
            WHERE f.contractId = :contractId AND f.executedAt >= :since
            """)
    long sumVolumeSince(@Param("contractId") UUID contractId, @Param("since") Instant since);

    @Query("""
            SELECT COALESCE(SUM(f.totalPriceUsd), 0) FROM ExchangeFill f
            WHERE f.contractId = :contractId AND f.executedAt >= :since
            """)
    BigDecimal sumNotionalSince(@Param("contractId") UUID contractId, @Param("since") Instant since);
}
