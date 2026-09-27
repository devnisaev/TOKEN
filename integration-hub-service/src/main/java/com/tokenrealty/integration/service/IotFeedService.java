package com.tokenrealty.integration.service;

import com.tokenrealty.integration.client.PropertyRegistryClient;
import com.tokenrealty.integration.dto.IntegrationDtos.*;
import com.tokenrealty.integration.entity.IotReading;
import com.tokenrealty.integration.repository.IotReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IotFeedService {

    private final IotReadingRepository iotReadingRepository;
    private final PropertyRegistryClient propertyRegistryClient;
    private final Clock clock;

    @Transactional
    public IotReadingResponse ingest(IngestIotReadingRequest request) {
        IotReading reading = iotReadingRepository.save(IotReading.builder()
                .flatId(request.flatId())
                .provider(request.provider())
                .occupancyPct(request.occupancyPct())
                .recordedAt(clock.instant())
                .build());
        propertyRegistryClient.updateFlatOccupancy(request.flatId(), request.occupancyPct());
        return toResponse(reading);
    }

    public List<IotReadingResponse> listByFlat(UUID flatId) {
        return iotReadingRepository.findByFlatIdOrderByRecordedAtDesc(flatId).stream()
                .map(this::toResponse)
                .toList();
    }

    private IotReadingResponse toResponse(IotReading reading) {
        return IotReadingResponse.builder()
                .id(reading.getId())
                .flatId(reading.getFlatId())
                .provider(reading.getProvider())
                .occupancyPct(reading.getOccupancyPct())
                .recordedAt(reading.getRecordedAt())
                .build();
    }
}
