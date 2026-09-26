package com.tokenrealty.rental.integration;

import com.tokenrealty.rental.client.PaymentClient;
import com.tokenrealty.rental.dto.RentalDtos.RecordRentPaymentRequest;
import com.tokenrealty.rental.entity.Lease;
import com.tokenrealty.rental.repository.LeaseRepository;
import com.tokenrealty.rental.repository.RentPaymentRepository;
import com.tokenrealty.rental.service.RentPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Rent payment integration test")
class RentPaymentIntegrationTest {

    @Autowired RentPaymentService rentPaymentService;
    @Autowired LeaseRepository leaseRepository;
    @Autowired RentPaymentRepository rentPaymentRepository;

    @MockitoBean PaymentClient paymentClient;

    private UUID leaseId;
    private UUID tenantId;

    @BeforeEach
    void seedLease() {
        tenantId = UUID.randomUUID();
        Lease lease = leaseRepository.save(Lease.builder()
                .flatId(UUID.randomUUID())
                .tenantId(tenantId)
                .tenantWallet("0xTenant")
                .spvRecipientId(UUID.randomUUID())
                .spvWallet("0xSPV")
                .monthlyRentUsd(new BigDecimal("650.00"))
                .startDate(LocalDate.now().withDayOfMonth(1))
                .endDate(LocalDate.now().plusYears(1))
                .status(Lease.LeaseStatus.ACTIVE)
                .build());
        leaseId = lease.getId();

        when(paymentClient.recordRentCollection(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PaymentClient.PayoutResponse(UUID.randomUUID()));
    }

    @Test
    @DisplayName("record rent payment persists row and calls Payment Service")
    void recordRentPayment_persistsAndCallsPayment() {
        var response = rentPaymentService.record(
                new RecordRentPaymentRequest(leaseId, "2025-09", new BigDecimal("650.00")),
                tenantId,
                true);

        assertThat(response.leaseId()).isEqualTo(leaseId);
        assertThat(response.period()).isEqualTo("2025-09");
        assertThat(rentPaymentRepository.findByLeaseIdOrderByPeriodDesc(leaseId)).hasSize(1);
    }
}
