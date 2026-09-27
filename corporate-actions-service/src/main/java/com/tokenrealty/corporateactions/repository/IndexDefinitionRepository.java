package com.tokenrealty.corporateactions.repository;

import com.tokenrealty.corporateactions.entity.IndexDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IndexDefinitionRepository extends JpaRepository<IndexDefinition, UUID> {

    Optional<IndexDefinition> findBySymbol(String symbol);
}
