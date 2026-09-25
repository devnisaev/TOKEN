package com.tokenrealty.registry.repository;

import com.tokenrealty.registry.entity.Valuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ValuationRepository extends JpaRepository<Valuation, UUID> {

    List<Valuation> findByFlatIdOrderByValuationDateDesc(UUID flatId);

    Optional<Valuation> findByFlatIdAndIsCurrentTrue(UUID flatId);

    @Modifying
    @Query("UPDATE Valuation v SET v.isCurrent = false WHERE v.flat.id = :flatId AND v.isCurrent = true")
    void deactivateCurrentValuations(@Param("flatId") UUID flatId);
}
