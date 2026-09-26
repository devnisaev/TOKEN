package com.tokenrealty.search.repository;

import com.tokenrealty.search.entity.ListingIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingIndexRepository extends JpaRepository<ListingIndex, UUID> {

    Optional<ListingIndex> findByListingId(UUID listingId);

    List<ListingIndex> findByFlatId(UUID flatId);

    @Query("""
            SELECT l FROM ListingIndex l
            WHERE (:q IS NULL OR :q = '' OR LOWER(l.searchText) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:listingType IS NULL OR :listingType = '' OR l.listingType = :listingType)
              AND (:minPrice IS NULL OR l.priceUsd >= :minPrice)
              AND (:maxPrice IS NULL OR l.priceUsd <= :maxPrice)
            """)
    Page<ListingIndex> search(
            @Param("q") String q,
            @Param("listingType") String listingType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
