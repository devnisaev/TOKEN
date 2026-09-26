package com.tokenrealty.rental.mapper;

import com.tokenrealty.rental.dto.RentalDtos.*;
import com.tokenrealty.rental.entity.Lease;
import com.tokenrealty.rental.entity.RentPayment;
import org.springframework.stereotype.Component;

@Component
public class RentalMapper {

    public LeaseResponse toLeaseResponse(Lease lease) {
        return new LeaseResponse(
                lease.getId(),
                lease.getFlatId(),
                lease.getTenantId(),
                lease.getTenantWallet(),
                lease.getSpvRecipientId(),
                lease.getSpvWallet(),
                lease.getMonthlyRentUsd(),
                lease.getStartDate(),
                lease.getEndDate(),
                lease.getStatus(),
                lease.getCreatedAt()
        );
    }

    public RentPaymentResponse toRentPaymentResponse(RentPayment payment) {
        return new RentPaymentResponse(
                payment.getId(),
                payment.getLeaseId(),
                payment.getFlatId(),
                payment.getPeriod(),
                payment.getAmount(),
                payment.getPayoutId(),
                payment.getStatus(),
                payment.getPaidAt()
        );
    }
}
