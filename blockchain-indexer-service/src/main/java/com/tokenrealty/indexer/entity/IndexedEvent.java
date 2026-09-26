package com.tokenrealty.indexer.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;

@Entity
@Table(name = "indexed_events", indexes = {
        @Index(name = "uk_indexed_events_tx_log", columnList = "tx_hash, log_index", unique = true),
        @Index(name = "idx_indexed_events_contract", columnList = "contract_address")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class IndexedEvent extends BaseEntity {

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "contract_address", nullable = false, length = 66)
    private String contractAddress;

    @Column(name = "tx_hash", nullable = false, length = 66)
    private String txHash;

    @Column(name = "log_index", nullable = false)
    private Integer logIndex;

    @Column(name = "block_number", nullable = false)
    private BigInteger blockNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
}
