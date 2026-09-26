package com.tokenrealty.gateway.client;

import com.tokenrealty.web.rest.DownstreamRestClientSupport;
import com.tokenrealty.web.rest.DownstreamServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class RentalClient extends DownstreamRestClientSupport {

    public RentalClient(@Qualifier("rentalRestClient") RestClient restClient) {
        super(restClient);
    }

    public List<LeaseView> listLeasesByTenantId(UUID tenantId) {
        LeaseView[] body = get(
                "/v1/leases?tenantId={tenantId}",
                LeaseView[].class,
                DownstreamServices.RENTAL,
                tenantId);
        return body == null ? List.of() : List.of(body);
    }

    public List<RentPaymentView> listRentPayments(UUID leaseId) {
        RentPaymentView[] body = get(
                "/v1/rent-payments?leaseId={leaseId}",
                RentPaymentView[].class,
                DownstreamServices.RENTAL,
                leaseId);
        return body == null ? List.of() : List.of(body);
    }

    public record LeaseView(
            UUID id,
            UUID flatId,
            UUID tenantId,
            String tenantWallet,
            UUID spvRecipientId,
            String spvWallet,
            BigDecimal monthlyRentUsd,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            Instant createdAt
    ) {
    }

    public record RentPaymentView(
            UUID id,
            UUID leaseId,
            UUID flatId,
            String period,
            BigDecimal amount,
            UUID payoutId,
            String status,
            Instant paidAt
    ) {
    }
}
