package com.tokenrealty.rental.service;

import com.tokenrealty.rental.dto.RentalDtos.*;
import com.tokenrealty.rental.entity.Lease;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.rental.mapper.RentalMapper;
import com.tokenrealty.rental.repository.LeaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final RentalMapper mapper;

    public LeaseResponse findById(UUID id) {
        return mapper.toLeaseResponse(getLease(id));
    }

    public OccupancyResponse getOccupancy(UUID flatId) {
        return leaseRepository.findFirstByFlatIdAndStatusOrderByStartDateDesc(flatId, Lease.LeaseStatus.ACTIVE)
                .map(lease -> new OccupancyResponse(
                        flatId, true, lease.getId(), lease.getTenantId(), lease.getEndDate()))
                .orElse(new OccupancyResponse(flatId, false, null, null, null));
    }

    @Transactional
    public LeaseResponse create(CreateLeaseRequest request) {
        if (leaseRepository.existsByFlatIdAndStatus(request.flatId(), Lease.LeaseStatus.ACTIVE)) {
            throw new ConflictException("Flat already has an active lease");
        }
        Lease lease = Lease.builder()
                .flatId(request.flatId())
                .tenantId(request.tenantId())
                .tenantWallet(request.tenantWallet())
                .spvRecipientId(request.spvRecipientId())
                .spvWallet(request.spvWallet())
                .monthlyRentUsd(request.monthlyRentUsd())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(Lease.LeaseStatus.ACTIVE)
                .build();
        return mapper.toLeaseResponse(leaseRepository.save(lease));
    }

    Lease getLease(UUID id) {
        return leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", id));
    }
}
