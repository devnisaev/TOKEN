package com.tokenrealty.gateway.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Component
public class GovernanceClient extends DownstreamRestClientSupport {

    public GovernanceClient(@Qualifier("governanceRestClient") RestClient restClient) {
        super(restClient);
    }

    public SpringPage<ProposalView> listProposals(Pageable pageable) {
        return get(
                uriBuilder -> buildPageUri(uriBuilder.path("/v1/governance/proposals"), pageable),
                new ParameterizedTypeReference<>() {
                },
                DownstreamServices.GOVERNANCE);
    }

    public ProposalView createProposal(CreateProposalRequest request) {
        return post("/v1/governance/proposals", request, ProposalView.class, DownstreamServices.GOVERNANCE);
    }

    public ProposalView castVote(UUID proposalId, CastVoteRequest request) {
        return post(
                "/v1/governance/proposals/{id}/votes",
                request,
                ProposalView.class,
                DownstreamServices.GOVERNANCE,
                proposalId);
    }

    public ProposalView closeProposal(UUID proposalId) {
        return post(
                "/v1/governance/proposals/{id}/close",
                null,
                ProposalView.class,
                DownstreamServices.GOVERNANCE,
                proposalId);
    }

    private static URI buildPageUri(UriBuilder uriBuilder, Pageable pageable) {
        var builder = uriBuilder;
        builder.queryParam("page", pageable.getPageNumber());
        builder.queryParam("size", pageable.getPageSize());
        pageable.getSort().forEach(order ->
                builder.queryParam("sort", order.getProperty() + "," + order.getDirection().name().toLowerCase()));
        return builder.build();
    }

    public record SpringPage<T>(
            java.util.List<T> content,
            long totalElements,
            int totalPages,
            int size,
            int number
    ) {
    }

    public record CreateProposalRequest(
            UUID flatId,
            String title,
            String description,
            BigDecimal quorumPct,
            Instant closesAt
    ) {
    }

    public record CastVoteRequest(UUID investorId, Boolean support) {
    }

    public record ProposalView(
            UUID id,
            UUID flatId,
            String title,
            String description,
            String status,
            BigDecimal quorumPct,
            int votesFor,
            int votesAgainst,
            Instant closesAt,
            Instant closedAt,
            Instant createdAt
    ) {
    }
}
