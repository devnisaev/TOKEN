package com.tokenrealty.valuation.integration;

import com.tokenrealty.valuation.dto.ValuationDtos.NavSnapshotResponse;
import com.tokenrealty.valuation.dto.ValuationDtos.SubmitValuationRequest;
import com.tokenrealty.valuation.dto.ValuationDtos.ValuationRequestResponse;
import com.tokenrealty.valuation.entity.ValuationRequestStatus;
import com.tokenrealty.valuation.repository.NavSnapshotRepository;
import com.tokenrealty.valuation.service.ValuationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Valuation workflow integration test")
class ValuationWorkflowTest {

    @Autowired ValuationService valuationService;
    @Autowired NavSnapshotRepository navSnapshotRepository;

    @Test
    void submitThenApprove_createsNavSnapshot() {
        UUID appraiserId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID buildingId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();

        ValuationRequestResponse submitted = valuationService.submit(
                new SubmitValuationRequest(buildingId, flatId, new BigDecimal("1000000.00"), 1000L, "Annual appraisal"),
                appraiserId);

        assertThat(submitted.status()).isEqualTo(ValuationRequestStatus.PENDING);
        assertThat(submitted.submittedBy()).isEqualTo(appraiserId);

        ValuationRequestResponse approved = valuationService.approve(submitted.id(), adminId);

        assertThat(approved.status()).isEqualTo(ValuationRequestStatus.APPROVED);
        assertThat(approved.reviewedBy()).isEqualTo(adminId);
        assertThat(approved.navSnapshotId()).isNotNull();

        NavSnapshotResponse nav = valuationService.getLatestNav(flatId);

        assertThat(nav.flatId()).isEqualTo(flatId);
        assertThat(nav.buildingId()).isEqualTo(buildingId);
        assertThat(nav.valueUsd()).isEqualByComparingTo("1000000.00");
        assertThat(nav.totalTokens()).isEqualTo(1000L);
        assertThat(nav.navPerTokenUsd()).isEqualByComparingTo("1000.00000000");
        assertThat(navSnapshotRepository.count()).isEqualTo(1);
    }
}
