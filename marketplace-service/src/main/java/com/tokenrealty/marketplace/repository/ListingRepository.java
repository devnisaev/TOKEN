package com.tokenrealty.marketplace.repository;

import com.tokenrealty.marketplace.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {

    Page<Listing> findByStatus(Listing.ListingStatus status, Pageable pageable);

    Page<Listing> findByFlatId(UUID flatId, Pageable pageable);

    Optional<Listing> findByFlatIdAndStatus(UUID flatId, Listing.ListingStatus status);
}
