package com.tokenrealty.corporateactions.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "index_dividend_accruals", indexes = {
        @Index(name = "idx_index_dividend_index", columnList = "index_id"),
        @Index(name = "idx_index_dividend_source", columnList = "source_payout_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexDividendAccrual extends BaseEntity {

    @Column(name = "index_id", nullable = false)
    private UUID indexId;

    @Column(name = "constituent_contract_id", nullable = false)
    private UUID constituentContractId;

    @Column(name = "source_payout_id", nullable = false)
    private UUID sourcePayoutId;

    @Column(name = "constituent_payout_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal constituentPayoutUsd;

    @Column(name = "index_share_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal indexShareUsd;

    @Column(name = "weight_bps", nullable = false)
    private int weightBps;
}
