package com.tokenrealty.rental.service;

import com.tokenrealty.rental.client.PaymentClient;
import com.tokenrealty.rental.dto.RentalDtos.*;
import com.tokenrealty.rental.entity.Lease;
import com.tokenrealty.rental.entity.RentPayment;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.rental.mapper.RentalMapper;
import com.tokenrealty.rental.repository.RentPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentPaymentService {

    private final RentPaymentRepository rentPaymentRepository;
    private final LeaseService leaseService;
    private final PaymentClient paymentClient;
    private final RentalMapper mapper;

    @Transactional
    public RentPaymentResponse record(RecordRentPaymentRequest request) {
        Lease lease = leaseService.getLease(request.leaseId());
        if (lease.getStatus() != Lease.LeaseStatus.ACTIVE) {
            throw new ConflictException("Lease is not active");
        }
        if (rentPaymentRepository.existsByLeaseIdAndPeriod(request.leaseId(), request.period())) {
            throw new ConflictException("Rent already recorded for period " + request.period());
        }

        PaymentClient.PayoutResponse payout = paymentClient.recordRentCollection(
                lease.getId(),
                lease.getFlatId(),
                lease.getTenantId(),
                lease.getSpvRecipientId(),
                lease.getSpvWallet(),
                request.amount(),
                request.period()
        );

        RentPayment payment = RentPayment.builder()
                .leaseId(lease.getId())
                .flatId(lease.getFlatId())
                .period(request.period())
                .amount(request.amount())
                .payoutId(payout.id())
                .status(RentPayment.RentPaymentStatus.PAID)
                .paidAt(Instant.now())
                .build();
        return mapper.toRentPaymentResponse(rentPaymentRepository.save(payment));
    }
}
