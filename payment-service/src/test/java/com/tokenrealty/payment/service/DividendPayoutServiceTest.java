package com.tokenrealty.payment.service;

import com.tokenrealty.payment.dto.PaymentDtos.PayoutResponse;
import com.tokenrealty.payment.entity.Payout;
import com.tokenrealty.payment.kafka.command.DividendDistributedCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DividendPayoutService unit tests")
class DividendPayoutServiceTest {

    @Mock PayoutService payoutService;
    @InjectMocks DividendPayoutService dividendPayoutService;

    @Test
    @DisplayName("createHolderPayouts creates one payout per holder")
    void createHolderPayouts_createsPayoutForEachHolder() {
        UUID contractId = UUID.randomUUID();
        var command = new DividendDistributedCommand(
                UUID.randomUUID(),
                contractId,
                UUID.randomUUID(),
                "2025-09",
                List.of(
                        new DividendDistributedCommand.HolderPayout(
                                UUID.randomUUID(), "0xA", BigDecimal.TEN),
                        new DividendDistributedCommand.HolderPayout(
                                UUID.randomUUID(), "0xB", BigDecimal.ONE)));

        when(payoutService.create(any())).thenReturn(
                PayoutResponse.builder().id(UUID.randomUUID()).status(Payout.PayoutStatus.COMPLETED).build());

        dividendPayoutService.createHolderPayouts(command);

        verify(payoutService, times(2)).create(any());
    }
}
