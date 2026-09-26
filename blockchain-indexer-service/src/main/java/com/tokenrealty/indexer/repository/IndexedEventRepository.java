package com.tokenrealty.indexer.repository;

import com.tokenrealty.indexer.entity.IndexedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IndexedEventRepository extends JpaRepository<IndexedEvent, UUID> {

    boolean existsByTxHashAndLogIndex(String txHash, Integer logIndex);

    Page<IndexedEvent> findAllByOrderByBlockNumberDesc(Pageable pageable);
}
