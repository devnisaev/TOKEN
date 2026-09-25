package com.tokenrealty.issuance.repository;

import com.tokenrealty.issuance.entity.TokenHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenHolderRepository extends JpaRepository<TokenHolder, UUID> {

    List<TokenHolder> findByTokenContractId(UUID contractId);

    Page<TokenHolder> findByTokenContractId(UUID contractId, Pageable pageable);

    Optional<TokenHolder> findByTokenContractIdAndInvestorId(UUID contractId, UUID investorId);

    Optional<TokenHolder> findByTokenContractIdAndWalletAddress(UUID contractId, String walletAddress);

    List<TokenHolder> findByInvestorId(UUID investorId);

    boolean existsByTokenContractIdAndInvestorId(UUID contractId, UUID investorId);

    @Query("SELECT COUNT(h) FROM TokenHolder h WHERE h.tokenContract.id = :contractId AND h.balance > 0")
    long countActiveHolders(@Param("contractId") UUID contractId);

    @Modifying
    @Query("UPDATE TokenHolder h SET h.balance = :balance, h.ownershipPercentage = :pct WHERE h.id = :id")
    void updateBalance(@Param("id") UUID id, @Param("balance") Long balance, @Param("pct") java.math.BigDecimal pct);
}
