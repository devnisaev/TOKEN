package com.tokenrealty.governance.repository;

import com.tokenrealty.governance.entity.GovernanceProposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GovernanceProposalRepository extends JpaRepository<GovernanceProposal, UUID> {

    Page<GovernanceProposal> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
