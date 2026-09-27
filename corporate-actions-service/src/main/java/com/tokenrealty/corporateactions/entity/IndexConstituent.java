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

import java.util.UUID;

@Entity
@Table(name = "index_constituents", indexes = {
        @Index(name = "idx_index_constituents_index", columnList = "index_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexConstituent extends BaseEntity {

    @Column(name = "index_id", nullable = false)
    private UUID indexId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "weight_bps", nullable = false)
    private int weightBps;
}
