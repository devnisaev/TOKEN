package com.tokenrealty.corporateactions.repository;

import com.tokenrealty.corporateactions.entity.IndexDividendAccrual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IndexDividendAccrualRepository extends JpaRepository<IndexDividendAccrual, UUID> {

    List<IndexDividendAccrual> findByIndexIdOrderByCreatedAtDesc(UUID indexId);

    boolean existsBySourcePayoutId(UUID sourcePayoutId);
}
