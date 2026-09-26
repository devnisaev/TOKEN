package com.tokenrealty.valuation.service;

import com.tokenrealty.valuation.dto.ValuationDtos.NavSnapshotResponse;
import com.tokenrealty.valuation.dto.ValuationDtos.RejectValuationRequest;
import com.tokenrealty.valuation.dto.ValuationDtos.SubmitValuationRequest;
import com.tokenrealty.valuation.dto.ValuationDtos.ValuationRequestResponse;
import com.tokenrealty.valuation.entity.NavSnapshot;
import com.tokenrealty.valuation.entity.ValuationRequest;
import com.tokenrealty.valuation.entity.ValuationRequestStatus;
import com.tokenrealty.valuation.kafka.port.ValuationEventPublisher;
import com.tokenrealty.valuation.repository.NavSnapshotRepository;
import com.tokenrealty.valuation.repository.ValuationRequestRepository;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValuationService {

    private static final int NAV_SCALE = 8;

    private final ValuationRequestRepository valuationRequestRepository;
    private final NavSnapshotRepository navSnapshotRepository;
    private final ValuationEventPublisher valuationEventPublisher;
    private final Clock clock;

    @Transactional
    public ValuationRequestResponse submit(SubmitValuationRequest request, UUID submittedBy) {
        ValuationRequest entity = ValuationRequest.builder()
                .buildingId(request.buildingId())
                .flatId(request.flatId())
                .valueUsd(request.valueUsd())
                .totalTokens(request.totalTokens())
                .status(ValuationRequestStatus.PENDING)
                .submittedBy(submittedBy)
                .notes(request.notes())
                .build();
        return toResponse(valuationRequestRepository.save(entity));
    }

    @Transactional
    public ValuationRequestResponse approve(UUID id, UUID reviewedBy) {
        ValuationRequest request = findRequest(id);
        if (request.getStatus() != ValuationRequestStatus.PENDING) {
            raiseValidation("Only pending valuation requests can be approved");
        }

        Instant approvedAt = clock.instant();
        BigDecimal navPerTokenUsd = calculateNav(request.getValueUsd(), request.getTotalTokens());

        NavSnapshot snapshot = navSnapshotRepository.save(NavSnapshot.builder()
                .flatId(request.getFlatId())
                .buildingId(request.getBuildingId())
                .valuationRequestId(request.getId())
                .valueUsd(request.getValueUsd())
                .totalTokens(request.getTotalTokens())
                .navPerTokenUsd(navPerTokenUsd)
                .approvedAt(approvedAt)
                .build());

        request.setStatus(ValuationRequestStatus.APPROVED);
        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(approvedAt);
        request.setNavSnapshotId(snapshot.getId());
        valuationRequestRepository.save(request);

        valuationEventPublisher.publishValuationUpdated(
                new ValuationEventPublisher.ValuationUpdatedEvent(
                        request.getFlatId(),
                        request.getBuildingId(),
                        request.getId(),
                        request.getValueUsd(),
                        request.getTotalTokens(),
                        navPerTokenUsd,
                        approvedAt));

        valuationEventPublisher.publishValuationApproved(
                new ValuationEventPublisher.ValuationApprovedEvent(
                        request.getFlatId(),
                        request.getBuildingId(),
                        request.getId(),
                        request.getValueUsd(),
                        request.getTotalTokens(),
                        navPerTokenUsd,
                        approvedAt,
                        reviewedBy));

        return toResponse(request);
    }

    @Transactional
    public ValuationRequestResponse reject(UUID id, UUID reviewedBy, RejectValuationRequest body) {
        ValuationRequest request = findRequest(id);
        if (request.getStatus() != ValuationRequestStatus.PENDING) {
            raiseValidation("Only pending valuation requests can be rejected");
        }

        request.setStatus(ValuationRequestStatus.REJECTED);
        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(clock.instant());
        request.setRejectionReason(body != null ? body.reason() : null);
        return toResponse(valuationRequestRepository.save(request));
    }

    public List<ValuationRequestResponse> findByBuilding(UUID buildingId) {
        return valuationRequestRepository.findByBuildingIdOrderByCreatedAtDesc(buildingId).stream()
                .map(this::toResponse)
                .toList();
    }

    public NavSnapshotResponse getLatestNav(UUID flatId) {
        NavSnapshot snapshot = navSnapshotRepository.findTopByFlatIdOrderByApprovedAtDesc(flatId)
                .orElseThrow(() -> new ResourceNotFoundException("NavSnapshot", flatId));
        return toNavResponse(snapshot);
    }

    static BigDecimal calculateNav(BigDecimal valueUsd, long totalTokens) {
        return valueUsd.divide(BigDecimal.valueOf(totalTokens), NAV_SCALE, RoundingMode.HALF_UP);
    }

    private ValuationRequest findRequest(UUID id) {
        return valuationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ValuationRequest", id));
    }

    private ValuationRequestResponse toResponse(ValuationRequest request) {
        return new ValuationRequestResponse(
                request.getId(),
                request.getBuildingId(),
                request.getFlatId(),
                request.getValueUsd(),
                request.getTotalTokens(),
                request.getStatus(),
                request.getSubmittedBy(),
                request.getReviewedBy(),
                request.getNotes(),
                request.getRejectionReason(),
                request.getReviewedAt(),
                request.getNavSnapshotId(),
                request.getCreatedAt());
    }

    private NavSnapshotResponse toNavResponse(NavSnapshot snapshot) {
        return new NavSnapshotResponse(
                snapshot.getId(),
                snapshot.getFlatId(),
                snapshot.getBuildingId(),
                snapshot.getValuationRequestId(),
                snapshot.getValueUsd(),
                snapshot.getTotalTokens(),
                snapshot.getNavPerTokenUsd(),
                snapshot.getApprovedAt(),
                snapshot.getCreatedAt());
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
