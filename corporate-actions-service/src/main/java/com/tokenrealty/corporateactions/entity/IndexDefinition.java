package com.tokenrealty.corporateactions.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "index_definitions", indexes = {
        @Index(name = "idx_index_definitions_symbol", columnList = "symbol", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexDefinition extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20, unique = true)
    private String symbol;

    @Column(length = 500)
    private String description;

    @Column(name = "index_contract_id")
    private UUID indexContractId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private IndexStatus status = IndexStatus.DRAFT;

    public enum IndexStatus {
        DRAFT, ACTIVE, REBALANCING, CLOSED
    }
}
