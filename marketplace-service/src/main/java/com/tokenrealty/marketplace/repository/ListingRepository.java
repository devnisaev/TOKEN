package com.tokenrealty.marketplace.repository;

import com.tokenrealty.marketplace.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {

    Page<Listing> findByStatus(Listing.ListingStatus status, Pageable pageable);

    Page<Listing> findByFlatId(UUID flatId, Pageable pageable);

    Optional<Listing> findByFlatIdAndStatus(UUID flatId, Listing.ListingStatus status);

    @Query("""
            SELECT l FROM Listing l
            WHERE (:status IS NULL OR l.status = :status)
              AND (:flatId IS NULL OR l.flatId = :flatId)
              AND (:propertyCategory IS NULL OR :propertyCategory = '' OR l.propertyCategory = :propertyCategory)
              AND (:operatingModel IS NULL OR :operatingModel = '' OR l.operatingModel = :operatingModel)
              AND (:liquidityTier IS NULL OR :liquidityTier = '' OR l.liquidityTier = :liquidityTier)
            """)
    Page<Listing> search(
            @Param("status") Listing.ListingStatus status,
            @Param("flatId") UUID flatId,
            @Param("propertyCategory") String propertyCategory,
            @Param("operatingModel") String operatingModel,
            @Param("liquidityTier") String liquidityTier,
            Pageable pageable);
}
