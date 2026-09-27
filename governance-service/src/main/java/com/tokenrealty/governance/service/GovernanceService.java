package com.tokenrealty.governance.service;

import com.tokenrealty.governance.dto.GovernanceDtos.CastVoteRequest;
import com.tokenrealty.governance.dto.GovernanceDtos.CreateProposalRequest;
import com.tokenrealty.governance.dto.GovernanceDtos.ProposalView;
import com.tokenrealty.governance.entity.GovernanceProposal;
import com.tokenrealty.governance.entity.GovernanceProposalStatus;
import com.tokenrealty.governance.entity.GovernanceVote;
import com.tokenrealty.governance.kafka.events.ProposalClosedEvent;
import com.tokenrealty.governance.kafka.port.ProposalClosedPublisher;
import com.tokenrealty.governance.repository.GovernanceProposalRepository;
import com.tokenrealty.governance.repository.GovernanceVoteRepository;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GovernanceService {

    private final GovernanceProposalRepository proposalRepository;
    private final GovernanceVoteRepository voteRepository;
    private final ProposalClosedPublisher proposalClosedPublisher;
    private final Clock clock;

    @Transactional
    public ProposalView createProposal(CreateProposalRequest request) {
        GovernanceProposal proposal = proposalRepository.save(GovernanceProposal.builder()
                .flatId(request.flatId())
                .title(request.title())
                .description(request.description())
                .status(GovernanceProposalStatus.OPEN)
                .quorumPct(request.quorumPct())
                .votesFor(0)
                .votesAgainst(0)
                .closesAt(request.closesAt())
                .build());
        return ProposalView.from(proposal);
    }

    @Transactional
    public ProposalView castVote(UUID proposalId, CastVoteRequest request) {
        GovernanceProposal proposal = findOpenProposal(proposalId);
        ensureVotingOpen(proposal);

        if (voteRepository.existsByProposal_IdAndInvestorId(proposalId, request.investorId())) {
            raiseConflict("Investor already voted on proposal: " + proposalId);
        }

        voteRepository.save(GovernanceVote.builder()
                .proposal(proposal)
                .investorId(request.investorId())
                .support(request.support())
                .build());

        if (request.support()) {
            proposal.setVotesFor(proposal.getVotesFor() + 1);
        } else {
            proposal.setVotesAgainst(proposal.getVotesAgainst() + 1);
        }
        return ProposalView.from(proposalRepository.save(proposal));
    }

    @Transactional
    public ProposalView closeProposal(UUID proposalId) {
        GovernanceProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found: " + proposalId));

        if (proposal.getStatus() != GovernanceProposalStatus.OPEN) {
            raiseValidation("Proposal is not open: " + proposalId);
        }

        Instant closedAt = clock.instant();
        proposal.setStatus(GovernanceProposalStatus.CLOSED);
        proposal.setClosedAt(closedAt);
        GovernanceProposal saved = proposalRepository.save(proposal);

        proposalClosedPublisher.publish(new ProposalClosedEvent(
                saved.getId(),
                saved.getFlatId(),
                saved.getQuorumPct(),
                saved.getVotesFor(),
                saved.getVotesAgainst(),
                closedAt));

        return ProposalView.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProposalView> listProposals(Pageable pageable) {
        return proposalRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ProposalView::from);
    }

    private GovernanceProposal findOpenProposal(UUID proposalId) {
        GovernanceProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found: " + proposalId));
        if (proposal.getStatus() != GovernanceProposalStatus.OPEN) {
            raiseValidation("Proposal is not open: " + proposalId);
        }
        return proposal;
    }

    private void ensureVotingOpen(GovernanceProposal proposal) {
        if (proposal.getClosesAt().isBefore(clock.instant())) {
            raiseValidation("Voting period has closed for proposal: " + proposal.getId());
        }
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }

    private static void raiseConflict(String message) {
        throw new ConflictException(message);
    }
}
