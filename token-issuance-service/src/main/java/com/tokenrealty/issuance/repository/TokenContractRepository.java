package com.tokenrealty.issuance.repository;

import com.tokenrealty.issuance.entity.TokenContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenContractRepository extends JpaRepository<TokenContract, UUID> {

    Optional<TokenContract> findByFlatId(UUID flatId);

    Optional<TokenContract> findByContractAddress(String contractAddress);

    Page<TokenContract> findByBuildingId(UUID buildingId, Pageable pageable);

    Page<TokenContract> findByStatus(TokenContract.ContractStatus status, Pageable pageable);

    boolean existsByFlatId(UUID flatId);

    @Query("SELECT t FROM TokenContract t WHERE t.status = 'ACTIVE'")
    Page<TokenContract> findAllActive(Pageable pageable);
}
