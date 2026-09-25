
package com.tokenrealty.registry.repository;

import com.tokenrealty.registry.entity.Building;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuildingRepository extends JpaRepository<Building, UUID> {

    Page<Building> findByStatus(Building.BuildingStatus status, Pageable pageable);

    Page<Building> findByCity(String city, Pageable pageable);

    @Query("SELECT b FROM Building b WHERE " +
            "LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.address) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.city) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Building> search(@Param("query") String query, Pageable pageable);

    @Query("SELECT b FROM Building b LEFT JOIN FETCH b.flats WHERE b.id = :id")
    Optional<Building> findByIdWithFlats(@Param("id") UUID id);

    boolean existsByAddressAndCity(String address, String city);
}
