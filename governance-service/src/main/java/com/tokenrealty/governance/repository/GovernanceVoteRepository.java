package com.tokenrealty.governance.repository;

import com.tokenrealty.governance.entity.GovernanceVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GovernanceVoteRepository extends JpaRepository<GovernanceVote, UUID> {

    boolean existsByProposal_IdAndInvestorId(UUID proposalId, UUID investorId);
}
