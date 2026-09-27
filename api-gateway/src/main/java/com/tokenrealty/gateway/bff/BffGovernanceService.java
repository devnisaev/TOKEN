package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.GovernanceClient;
import com.tokenrealty.gateway.client.GovernanceClient.ProposalView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BffGovernanceService {

    private final GovernanceClient governanceClient;
    private final BffPortfolioService portfolioService;

    public List<ProposalView> getInvestorGovernanceProposals(UUID investorId, Pageable pageable) {
        Set<UUID> heldFlatIds = portfolioService.getPortfolio(investorId).balance().tokenHoldings().stream()
                .map(h -> h.flatId())
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (heldFlatIds.isEmpty()) {
            return List.of();
        }
        return governanceClient.listProposals(pageable).content().stream()
                .filter(p -> "OPEN".equals(p.status()) && heldFlatIds.contains(p.flatId()))
                .toList();
    }
}
