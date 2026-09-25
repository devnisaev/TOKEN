package com.tokenrealty.issuance.repository;

import com.tokenrealty.issuance.entity.TokenTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenTransferRepository extends JpaRepository<TokenTransfer, UUID> {

    Page<TokenTransfer> findByTokenContractId(UUID contractId, Pageable pageable);

    Page<TokenTransfer> findByToAddress(String toAddress, Pageable pageable);

    Page<TokenTransfer> findByFromAddress(String fromAddress, Pageable pageable);

    Optional<TokenTransfer> findByTxHash(String txHash);

    List<TokenTransfer> findByTokenContractIdAndStatus(
            UUID contractId, TokenTransfer.TransferStatus status);
}