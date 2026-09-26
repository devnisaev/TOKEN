package com.tokenrealty.indexer.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "reconciliation_mismatches")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReconciliationMismatch extends BaseEntity {

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "contract_address", nullable = false, length = 66)
    private String contractAddress;

    @Column(name = "wallet_address", nullable = false, length = 66)
    private String walletAddress;

    @Column(name = "db_balance", nullable = false)
    private long dbBalance;

    @Column(name = "chain_balance", nullable = false)
    private long chainBalance;

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private boolean resolved = false;
}
