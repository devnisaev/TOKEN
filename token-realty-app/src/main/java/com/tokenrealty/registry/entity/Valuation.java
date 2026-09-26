package com.tokenrealty.registry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "valuations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Valuation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flat_id", nullable = false)
    private Flat flat;

    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;

    @Column(name = "value_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal valueUsd;

    @Column(name = "value_local_currency", precision = 18, scale = 2)
    private BigDecimal valueLocalCurrency;

    @Column(name = "local_currency", length = 3)
    private String localCurrency;  // ISO 4217 e.g. "KGS", "USD"

    @Column(name = "appraiser_name")
    private String appraiserName;

    @Column(name = "appraiser_license")
    private String appraiserLicense;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValuationMethod method;

    @Column(name = "annual_rental_income_usd", precision = 18, scale = 2)
    private BigDecimal annualRentalIncomeUsd;

    // Whether this is the current active valuation
    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private Boolean isCurrent = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum ValuationMethod {
        COMPARABLE_SALES,
        INCOME_APPROACH,
        COST_APPROACH,
        AUTOMATED  // AVM - automated valuation model
    }
}
