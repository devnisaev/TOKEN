package com.tokenrealty.payment.service;

import com.tokenrealty.payment.entity.PaymentCurrency;
import com.tokenrealty.payment.entity.WalletBalance;
import com.tokenrealty.payment.mapper.PaymentMapper;
import com.tokenrealty.payment.repository.WalletBalanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletBalanceService unit tests")
class WalletBalanceServiceTest {

    @Mock WalletBalanceRepository repository;
    @Mock PaymentMapper mapper;
    @InjectMocks WalletBalanceService walletBalanceService;

    @Test
    @DisplayName("holdForPayment moves available to held when sufficient balance")
    void holdForPayment_movesFunds() {
        UUID investorId = UUID.randomUUID();
        WalletBalance balance = WalletBalance.builder()
                .investorId(investorId)
                .currency(PaymentCurrency.USDC)
                .availableBalance(new BigDecimal("500.00"))
                .heldBalance(BigDecimal.ZERO)
                .build();

        when(repository.findByInvestorIdAndCurrency(investorId, PaymentCurrency.USDC))
                .thenReturn(Optional.of(balance));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        walletBalanceService.holdForPayment(investorId, new BigDecimal("100.00"), PaymentCurrency.USDC);

        assertThat(balance.getAvailableBalance()).isEqualByComparingTo("400.00");
        assertThat(balance.getHeldBalance()).isEqualByComparingTo("100.00");
        verify(repository).save(balance);
    }

    @Test
    @DisplayName("settleConfirmedPayment reduces held balance on confirm")
    void settleConfirmedPayment_reducesHeld() {
        UUID investorId = UUID.randomUUID();
        WalletBalance balance = WalletBalance.builder()
                .investorId(investorId)
                .currency(PaymentCurrency.USDC)
                .availableBalance(BigDecimal.ZERO)
                .heldBalance(new BigDecimal("100.00"))
                .build();

        when(repository.findByInvestorIdAndCurrency(investorId, PaymentCurrency.USDC))
                .thenReturn(Optional.of(balance));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        walletBalanceService.settleConfirmedPayment(investorId, new BigDecimal("100.00"), PaymentCurrency.USDC);

        assertThat(balance.getHeldBalance()).isEqualByComparingTo("0.00");
    }
}
