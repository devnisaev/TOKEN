package com.tokenrealty.indexer.repository;

import com.tokenrealty.indexer.entity.ReconciliationMismatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationMismatchRepository extends JpaRepository<ReconciliationMismatch, UUID> {

    List<ReconciliationMismatch> findTop50ByResolvedFalseOrderByCreatedAtDesc();
}
