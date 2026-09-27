package com.tokenrealty.governance.controller;

import com.tokenrealty.governance.dto.GovernanceDtos.CastVoteRequest;
import com.tokenrealty.governance.dto.GovernanceDtos.CreateProposalRequest;
import com.tokenrealty.governance.dto.GovernanceDtos.ProposalView;
import com.tokenrealty.governance.service.GovernanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/governance")
@RequiredArgsConstructor
public class GovernanceController {

    private final GovernanceService governanceService;

    @PostMapping("/proposals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProposalView createProposal(@Valid @RequestBody CreateProposalRequest request) {
        return governanceService.createProposal(request);
    }

    @PostMapping("/proposals/{id}/votes")
    public ProposalView castVote(
            @PathVariable UUID id,
            @Valid @RequestBody CastVoteRequest request) {
        return governanceService.castVote(id, request);
    }

    @PostMapping("/proposals/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ProposalView closeProposal(@PathVariable UUID id) {
        return governanceService.closeProposal(id);
    }

    @GetMapping("/proposals")
    public Page<ProposalView> listProposals(@PageableDefault(size = 20) Pageable pageable) {
        return governanceService.listProposals(pageable);
    }
}
