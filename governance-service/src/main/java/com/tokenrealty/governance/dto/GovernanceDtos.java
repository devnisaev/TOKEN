package com.tokenrealty.governance.dto;

import com.tokenrealty.governance.entity.GovernanceProposal;
import com.tokenrealty.governance.entity.GovernanceProposalStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class GovernanceDtos {

    private GovernanceDtos() {
    }

    public record CreateProposalRequest(
            @NotNull UUID flatId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 5000) String description,
            @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal quorumPct,
            @NotNull @Future Instant closesAt
    ) {
    }

    public record CastVoteRequest(
            @NotNull UUID investorId,
            @NotNull Boolean support
    ) {
    }

    public record ProposalView(
            UUID id,
            UUID flatId,
            String title,
            String description,
            GovernanceProposalStatus status,
            BigDecimal quorumPct,
            int votesFor,
            int votesAgainst,
            Instant closesAt,
            Instant closedAt,
            Instant createdAt
    ) {
        public static ProposalView from(GovernanceProposal proposal) {
            return new ProposalView(
                    proposal.getId(),
                    proposal.getFlatId(),
                    proposal.getTitle(),
                    proposal.getDescription(),
                    proposal.getStatus(),
                    proposal.getQuorumPct(),
                    proposal.getVotesFor(),
                    proposal.getVotesAgainst(),
                    proposal.getClosesAt(),
                    proposal.getClosedAt(),
                    proposal.getCreatedAt()
            );
        }
    }
}
