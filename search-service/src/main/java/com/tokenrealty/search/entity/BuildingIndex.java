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
@Table(name = "building_index")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingIndex extends BaseEntity {

    @Column(name = "building_id", nullable = false, unique = true)
    private UUID buildingId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "flat_count", nullable = false)
    private int flatCount;

    @Column(name = "latest_token_price_usd", precision = 19, scale = 2)
    private BigDecimal latestTokenPriceUsd;

    @Column(name = "latest_nav_per_token_usd", precision = 19, scale = 2)
    private BigDecimal latestNavPerTokenUsd;

    @Column(name = "search_text", nullable = false, length = 2000)
    private String searchText;

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "indexed_at", nullable = false)
    private Instant indexedAt;
}
