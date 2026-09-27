package com.tokenrealty.governance.integration;

import com.tokenrealty.governance.dto.GovernanceDtos.CastVoteRequest;
import com.tokenrealty.governance.dto.GovernanceDtos.CreateProposalRequest;
import com.tokenrealty.governance.entity.GovernanceProposalStatus;
import com.tokenrealty.governance.repository.GovernanceProposalRepository;
import com.tokenrealty.governance.repository.GovernanceVoteRepository;
import com.tokenrealty.governance.service.GovernanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GovernanceIntegrationTest {

    @Autowired GovernanceService governanceService;
    @Autowired GovernanceProposalRepository proposalRepository;
    @Autowired GovernanceVoteRepository voteRepository;

    @BeforeEach
    void clean() {
        voteRepository.deleteAll();
        proposalRepository.deleteAll();
    }

    @Test
    @DisplayName("create proposal, cast vote, and close proposal")
    void governanceLifecycle() {
        UUID flatId = UUID.randomUUID();
        UUID investorId = UUID.randomUUID();

        var created = governanceService.createProposal(new CreateProposalRequest(
                flatId,
                "Renovation budget",
                "Approve facade renovation for building A",
                new BigDecimal("51.00"),
                Instant.now().plus(7, ChronoUnit.DAYS)));

        assertThat(created.status()).isEqualTo(GovernanceProposalStatus.OPEN);
        assertThat(created.votesFor()).isZero();
        assertThat(created.votesAgainst()).isZero();

        var afterVote = governanceService.castVote(created.id(), new CastVoteRequest(investorId, true));

        assertThat(afterVote.votesFor()).isEqualTo(1);
        assertThat(afterVote.votesAgainst()).isZero();
        assertThat(voteRepository.count()).isEqualTo(1);

        var closed = governanceService.closeProposal(created.id());

        assertThat(closed.status()).isEqualTo(GovernanceProposalStatus.CLOSED);
        assertThat(closed.closedAt()).isNotNull();
        assertThat(closed.votesFor()).isEqualTo(1);
    }
}
