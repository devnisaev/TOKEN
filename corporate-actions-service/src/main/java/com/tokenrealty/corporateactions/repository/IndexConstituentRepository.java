package com.tokenrealty.corporateactions.repository;

import com.tokenrealty.corporateactions.entity.IndexConstituent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IndexConstituentRepository extends JpaRepository<IndexConstituent, UUID> {

    List<IndexConstituent> findByIndexIdOrderByWeightBpsDesc(UUID indexId);

    void deleteByIndexId(UUID indexId);

    List<IndexConstituent> findByContractId(UUID contractId);
}
