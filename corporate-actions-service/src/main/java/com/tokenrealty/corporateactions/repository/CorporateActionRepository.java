package com.tokenrealty.corporateactions.repository;

import com.tokenrealty.corporateactions.entity.CorporateAction;
import com.tokenrealty.corporateactions.entity.CorporateActionStatus;
import com.tokenrealty.corporateactions.entity.CorporateActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CorporateActionRepository extends JpaRepository<CorporateAction, UUID> {

    Page<CorporateAction> findByTypeOrderByCreatedAtDesc(CorporateActionType type, Pageable pageable);

    Optional<CorporateAction> findFirstByFlatIdAndPeriodAndTypeAndStatus(
            UUID flatId,
            String period,
            CorporateActionType type,
            CorporateActionStatus status);
}
