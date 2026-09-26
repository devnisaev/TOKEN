package com.tokenrealty.search.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listing_index")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingIndex extends BaseEntity {

    @Column(name = "listing_id", nullable = false, unique = true)
    private UUID listingId;

    @Column(name = "flat_id")
    private UUID flatId;

    @Column(name = "building_id")
    private UUID buildingId;

    @Column(name = "building_name", length = 200)
    private String buildingName;

    @Column(length = 100)
    private String city;

    @Column(name = "listing_type", length = 20)
    private String listingType;

    @Column(name = "price_usd", precision = 19, scale = 2)
    private BigDecimal priceUsd;

    @Column(name = "tokens_available")
    private Long tokensAvailable;

    @Column(name = "nav_per_token_usd", precision = 19, scale = 2)
    private BigDecimal navPerTokenUsd;

    @Column(name = "search_text", nullable = false, length = 2000)
    private String searchText;

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "indexed_at", nullable = false)
    private Instant indexedAt;
}
