package com.tokenrealty.rental.service;

import com.tokenrealty.rental.dto.RentalDtos.IngestOperatorRevenueRequest;
import com.tokenrealty.rental.dto.RentalDtos.OperatorRevenueResponse;
import com.tokenrealty.rental.entity.OperatorRevenueRecord;
import com.tokenrealty.rental.repository.OperatorRevenueRepository;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperatorRevenueService {

    private final OperatorRevenueRepository repository;

    public List<OperatorRevenueResponse> findByFlat(UUID flatId) {
        return repository.findByFlatIdOrderByPeriodEndDesc(flatId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OperatorRevenueResponse ingest(IngestOperatorRevenueRequest request, String sourceProvider) {
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new ValidationException("periodEnd must be on or after periodStart");
        }
        OperatorRevenueRecord saved = repository.save(OperatorRevenueRecord.builder()
                .buildingId(request.buildingId())
                .flatId(request.flatId())
                .periodStart(request.periodStart())
                .periodEnd(request.periodEnd())
                .grossRevenueUsd(request.grossRevenueUsd())
                .operatorName(request.operatorName())
                .sourceProvider(sourceProvider)
                .ingestedAt(Instant.now())
                .build());
        return toResponse(saved);
    }

    private OperatorRevenueResponse toResponse(OperatorRevenueRecord record) {
        return new OperatorRevenueResponse(
                record.getId(),
                record.getBuildingId(),
                record.getFlatId(),
                record.getPeriodStart(),
                record.getPeriodEnd(),
                record.getGrossRevenueUsd(),
                record.getOperatorName(),
                record.getSourceProvider(),
                record.getIngestedAt(),
                record.getCreatedAt());
    }
}
