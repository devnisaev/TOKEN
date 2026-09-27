package com.tokenrealty.integration.repository;

import com.tokenrealty.integration.entity.BridgeTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BridgeTransferRepository extends JpaRepository<BridgeTransfer, UUID> {

    List<BridgeTransfer> findByStatusOrderByCreatedAtDesc(BridgeTransfer.BridgeStatus status);
}
