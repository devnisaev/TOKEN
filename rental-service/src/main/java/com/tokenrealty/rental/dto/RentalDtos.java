package com.tokenrealty.rental.dto;

import com.tokenrealty.rental.entity.Lease;
import com.tokenrealty.rental.entity.RentPayment;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class RentalDtos {

    private RentalDtos() {
    }

    public record CreateLeaseRequest(
            @NotNull UUID flatId,
            @NotNull UUID tenantId,
            @NotBlank @Size(max = 66) String tenantWallet,
            @NotNull UUID spvRecipientId,
            @NotBlank @Size(max = 66) String spvWallet,
            @NotNull @DecimalMin("0.01") BigDecimal monthlyRentUsd,
            @NotNull LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record LeaseResponse(
            UUID id,
            UUID flatId,
            UUID tenantId,
            String tenantWallet,
            UUID spvRecipientId,
            String spvWallet,
            BigDecimal monthlyRentUsd,
            LocalDate startDate,
            LocalDate endDate,
            Lease.LeaseStatus status,
            Instant createdAt
    ) {
    }

    public record RecordRentPaymentRequest(
            @NotNull UUID leaseId,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period,
            @NotNull @DecimalMin("0.01") BigDecimal amount
    ) {
    }

    public record RentSummaryResponse(
            UUID flatId,
            String period,
            java.math.BigDecimal totalAmount
    ) {
    }

    public record RentPaymentResponse(
            UUID id,
            UUID leaseId,
            UUID flatId,
            String period,
            BigDecimal amount,
            UUID payoutId,
            RentPayment.RentPaymentStatus status,
            Instant paidAt
    ) {
    }

    public record OccupancyResponse(
            UUID flatId,
            boolean occupied,
            UUID activeLeaseId,
            UUID tenantId,
            LocalDate leaseEndDate
    ) {
    }

    public record CreateMaintenanceTicketRequest(
            @NotNull UUID leaseId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 2000) String description
    ) {
    }

    public record MaintenanceTicketResponse(
            UUID id,
            UUID leaseId,
            UUID flatId,
            UUID tenantId,
            String title,
            String description,
            com.tokenrealty.rental.entity.MaintenanceTicket.TicketStatus status,
            Instant createdAt
    ) {
    }
}
