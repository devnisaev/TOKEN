package com.tokenrealty.governance.entity;

import com.tokenrealty.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "governance_proposals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GovernanceProposal extends BaseEntity {

    @Column(name = "flat_id", nullable = false)
    private UUID flatId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GovernanceProposalStatus status;

    @Column(name = "quorum_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal quorumPct;

    @Column(name = "votes_for", nullable = false)
    private int votesFor;

    @Column(name = "votes_against", nullable = false)
    private int votesAgainst;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    @Column(name = "closed_at")
    private Instant closedAt;
}
