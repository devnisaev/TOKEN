package com.tokenrealty.marketplace.repository;

import com.tokenrealty.marketplace.entity.RfqQuote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RfqQuoteRepository extends JpaRepository<RfqQuote, UUID> {

    List<RfqQuote> findByRfqRequestIdOrderByCreatedAtDesc(UUID rfqRequestId);

    Optional<RfqQuote> findByIdAndRfqRequestId(UUID id, UUID rfqRequestId);
}
