package com.tokenrealty.wallet.service;

import com.tokenrealty.wallet.client.IssuanceClient;
import com.tokenrealty.wallet.client.PaymentClient;
import com.tokenrealty.wallet.crypto.WalletEncryptionService;
import com.tokenrealty.wallet.dto.WalletDtos.CreateCustodialWalletRequest;
import com.tokenrealty.wallet.entity.InvestorWallet;
import com.tokenrealty.wallet.repository.InvestorWalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService unit tests")
class WalletServiceTest {

    @Mock InvestorWalletRepository repository;
    @Mock WalletEncryptionService encryptionService;
    @Mock PaymentClient paymentClient;
    @Mock IssuanceClient issuanceClient;
    @Mock WalletAccessGuard accessGuard;
    @InjectMocks WalletService walletService;

    @Test
    @DisplayName("getAggregateBalance combines payment and issuance holdings")
    void getAggregateBalance_combinesSources() {
        UUID investorId = UUID.randomUUID();
        when(paymentClient.getWalletBalance(investorId))
                .thenReturn(new PaymentClient.WalletBalanceResponse(
                        investorId, "USDC", new BigDecimal("100.00"), BigDecimal.ZERO));
        when(issuanceClient.getHoldings(investorId)).thenReturn(List.of());
        when(repository.findByInvestorIdAndPrimaryTrue(investorId)).thenReturn(Optional.empty());

        var balance = walletService.getAggregateBalance(investorId);

        assertThat(balance.investorId()).isEqualTo(investorId);
        assertThat(balance.fiatBalances()).hasSize(1);
        verify(accessGuard).checkInvestorAccess(investorId);
    }

    @Test
    @DisplayName("createCustodialWallet persists encrypted key")
    void createCustodialWallet_encryptsKey() {
        UUID investorId = UUID.randomUUID();
        when(repository.existsByInvestorIdAndWalletType(investorId, InvestorWallet.WalletType.CUSTODIAL))
                .thenReturn(false);
        when(repository.findByInvestorIdAndPrimaryTrue(investorId)).thenReturn(Optional.empty());
        when(encryptionService.encrypt(any())).thenReturn("encrypted-key");
        when(repository.save(any(InvestorWallet.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = walletService.createCustodialWallet(
                new CreateCustodialWalletRequest(investorId, "Demo"));

        assertThat(response.walletAddress()).startsWith("0x");
        assertThat(response.walletType()).isEqualTo(InvestorWallet.WalletType.CUSTODIAL);
        verify(encryptionService).encrypt(any());
    }
}
