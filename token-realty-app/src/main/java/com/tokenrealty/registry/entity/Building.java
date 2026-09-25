package com.tokenrealty.registry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "buildings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Building extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String city;
    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "total_floors")
    private Integer totalFloors;

    @Column(name = "total_flats")
    private Integer totalFlats;

    @Column(name = "construction_year")
    private Integer constructionYear;

    @Column(name = "total_area_sqm")
    private Double totalAreaSqm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BuildingStatus status = BuildingStatus.PENDING_REVIEW;

    // GPS coordinates for map display
    private Double latitude;
    private Double longitude;

    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Flat> flats = new ArrayList<>();

    @OneToOne(mappedBy = "building", cascade = CascadeType.ALL)
    private SpvEntity spv;

    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PropertyDocument> documents = new ArrayList<>();

    public enum BuildingStatus {
        PENDING_REVIEW,    // Submitted, awaiting admin approval
        APPROVED,          // Approved, flats can be tokenized
        TOKENIZED,         // At least one flat has been tokenized
        SUSPENDED          // Temporarily halted
    }
}
