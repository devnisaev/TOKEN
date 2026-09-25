package com.tokenrealty.registry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"building_id", "flat_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    private Integer floor;

    @Column(name = "area_sqm")
    private Double areaSqm;

    @Column(name = "num_rooms")
    private Integer numRooms;

    @Column(name = "num_bathrooms")
    private Integer numBathrooms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FlatStatus status = FlatStatus.AVAILABLE;

    // Tokenization info — populated once tokens are issued
    @Column(name = "token_contract_address")
    private String tokenContractAddress;

    @Column(name = "total_tokens")
    private Long totalTokens;

    @Column(name = "token_price_usd", precision = 18, scale = 2)
    private BigDecimal tokenPriceUsd;

    @OneToMany(mappedBy = "flat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Valuation> valuations = new ArrayList<>();

    @OneToMany(mappedBy = "flat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PropertyDocument> documents = new ArrayList<>();

    public enum FlatStatus {
        AVAILABLE,        // Ready to be tokenized
        TOKENIZED,        // Token contract deployed
        FULLY_SOLD,       // All tokens sold
        SUSPENDED         // Temporarily halted
    }
}
