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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
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

    public List<LeaseResponse> listByTenantId(UUID tenantId) {
        return leaseRepository.findByTenantIdOrderByStartDateDesc(tenantId).stream()
                .map(mapper::toLeaseResponse)
                .toList();
    }

    public List<LeaseResponse> listActive() {
        return leaseRepository.findByStatus(Lease.LeaseStatus.ACTIVE).stream()
                .map(mapper::toLeaseResponse)
                .toList();
    }

    public List<LeaseExpiryAlertItem> listExpiringWithinDays(int withinDays) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(withinDays);
        return leaseRepository
                .findByStatusAndEndDateBetween(Lease.LeaseStatus.ACTIVE, today, cutoff)
                .stream()
                .filter(lease -> lease.getEndDate() != null)
                .map(lease -> toExpiryAlert(lease, today))
                .sorted(Comparator.comparingLong(LeaseExpiryAlertItem::daysUntilExpiry))
                .toList();
    }

    private LeaseExpiryAlertItem toExpiryAlert(Lease lease, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, lease.getEndDate());
        return new LeaseExpiryAlertItem(
                lease.getId(),
                lease.getFlatId(),
                lease.getTenantId(),
                lease.getEndDate(),
                Math.max(days, 0));
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

    public Lease getLease(UUID id) {
        return leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", id));
    }
}
