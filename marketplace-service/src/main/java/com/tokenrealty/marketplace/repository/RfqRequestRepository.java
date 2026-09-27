package com.tokenrealty.marketplace.repository;

import com.tokenrealty.marketplace.entity.RfqRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RfqRequestRepository extends JpaRepository<RfqRequest, UUID> {

    Page<RfqRequest> findByStatusOrderByCreatedAtDesc(RfqRequest.RfqStatus status, Pageable pageable);

    Page<RfqRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId, Pageable pageable);
}
