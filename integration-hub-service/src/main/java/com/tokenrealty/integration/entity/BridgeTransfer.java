package com.tokenrealty.integration.entity;

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
@Table(name = "bridge_transfers", indexes = {
        @Index(name = "idx_bridge_transfers_contract", columnList = "source_contract_id"),
        @Index(name = "idx_bridge_transfers_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BridgeTransfer extends BaseEntity {

    @Column(name = "source_contract_id", nullable = false)
    private UUID sourceContractId;

    @Column(name = "source_chain", nullable = false, length = 32)
    private String sourceChain;

    @Column(name = "target_chain", nullable = false, length = 32)
    private String targetChain;

    @Column(name = "investor_id", nullable = false)
    private UUID investorId;

    @Column(name = "wallet_address", nullable = false, length = 66)
    private String walletAddress;

    @Column(name = "token_amount", nullable = false)
    private long tokenAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BridgeStatus status = BridgeStatus.PENDING;

    @Column(name = "relay_tx_hash", length = 66)
    private String relayTxHash;

    public enum BridgeStatus {
        PENDING, RELAYED, FAILED, COMPLETED
    }
}
