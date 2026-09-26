package com.tokenrealty.search.repository;

import com.tokenrealty.search.entity.BuildingIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BuildingIndexRepository extends JpaRepository<BuildingIndex, UUID> {

    Optional<BuildingIndex> findByBuildingId(UUID buildingId);

    @Query("""
            SELECT b FROM BuildingIndex b
            WHERE (:q IS NULL OR :q = '' OR LOWER(b.searchText) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<BuildingIndex> search(@Param("q") String q, Pageable pageable);
}
