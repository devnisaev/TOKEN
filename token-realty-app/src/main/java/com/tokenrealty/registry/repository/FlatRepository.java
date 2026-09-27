package com.tokenrealty.registry.repository;

import com.tokenrealty.registry.entity.Building;
import com.tokenrealty.registry.entity.Flat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlatRepository extends JpaRepository<Flat, UUID> {

    List<Flat> findByBuildingId(UUID buildingId);

    Page<Flat> findByBuildingId(UUID buildingId, Pageable pageable);

    Page<Flat> findByStatus(Flat.FlatStatus status, Pageable pageable);

    Optional<Flat> findByBuildingIdAndFlatNumber(UUID buildingId, String flatNumber);

    boolean existsByBuildingIdAndFlatNumber(UUID buildingId, String flatNumber);

    @Query("SELECT f FROM Flat f WHERE f.building.id = :buildingId AND f.status = :status")
    List<Flat> findByBuildingIdAndStatus(
            @Param("buildingId") UUID buildingId,
            @Param("status") Flat.FlatStatus status);

    @Query("SELECT COUNT(f) FROM Flat f WHERE f.building.id = :buildingId AND f.status = 'TOKENIZED'")
    long countTokenizedByBuildingId(@Param("buildingId") UUID buildingId);

    @Query("""
            SELECT f FROM Flat f JOIN f.building b
            WHERE (:operatingModel IS NULL OR f.operatingModel = :operatingModel)
              AND (:liquidityTier IS NULL OR f.liquidityTier = :liquidityTier)
              AND (:propertyCategory IS NULL OR b.propertyCategory = :propertyCategory)
            """)
    Page<Flat> findAssetUnits(
            @Param("operatingModel") Flat.OperatingModel operatingModel,
            @Param("liquidityTier") Flat.LiquidityTier liquidityTier,
            @Param("propertyCategory") Building.PropertyCategory propertyCategory,
            Pageable pageable);
}
