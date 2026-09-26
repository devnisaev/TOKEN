package com.tokenrealty.compliance.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "investment_policies",
        uniqueConstraints = @UniqueConstraint(columnNames = "jurisdiction"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentPolicy extends BaseEntity {

    @Column(nullable = false, length = 2)
    private String jurisdiction;

    @Column(name = "min_investment_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal minInvestmentUsd;

    @Column(name = "max_investment_usd", precision = 19, scale = 2)
    private BigDecimal maxInvestmentUsd;

    @Column(name = "accredited_only", nullable = false)
    @Builder.Default
    private boolean accreditedOnly = false;
}
