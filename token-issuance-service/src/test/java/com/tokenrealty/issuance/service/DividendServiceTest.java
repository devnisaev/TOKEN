package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.entity.DividendPayment;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.issuance.kafka.port.DividendDistributedPublisher;
import com.tokenrealty.issuance.repository.DividendPaymentRepository;
import com.tokenrealty.issuance.repository.TokenContractRepository;
import com.tokenrealty.issuance.repository.TokenHolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DividendService unit tests")
class DividendServiceTest {

    @Mock DividendPaymentRepository dividendRepository;
    @Mock TokenContractRepository contractRepository;
    @Mock TokenHolderRepository holderRepository;
    @Mock DividendDistributedPublisher dividendDistributedPublisher;

    @InjectMocks DividendService service;

    private UUID contractId;
    private TokenContract contract;
    private TokenHolder holder1;
    private TokenHolder holder2;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();

        contract = TokenContract.builder()
                .tokenName("Bishkek City Plaza — Flat 101")
                .tokenSymbol("BKCP-101")
                .totalSupply(1000L)
                .tokenPriceUsd(BigDecimal.valueOf(45.00))
                .status(TokenContract.ContractStatus.ACTIVE)
                .flatId(UUID.randomUUID())
                .buildingId(UUID.randomUUID())
                .spvWalletAddress("0xSPV")
                .network("localhost")
                .chainId(31337L)
                .build();

        holder1 = TokenHolder.builder()
                .tokenContract(contract)
                .investorId(UUID.randomUUID())
                .walletAddress("0xInvestor1")
                .balance(700L)  // 70%
                .status(TokenHolder.HolderStatus.ACTIVE)
                .build();

        holder2 = TokenHolder.builder()
                .tokenContract(contract)
                .investorId(UUID.randomUUID())
                .walletAddress("0xInvestor2")
                .balance(300L)  // 30%
                .status(TokenHolder.HolderStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("distribute — creates payments pro-rata to all holders")
    void distribute_proRata() {
        var request = new DistributeDividendRequest(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                BigDecimal.valueOf(1000)  // $1000 gross rental income
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(dividendRepository.existsByTokenContractIdAndPeriodStartAndPeriodEnd(
                any(), any(), any())).thenReturn(false);
        when(holderRepository.findByTokenContractId(contractId))
                .thenReturn(List.of(holder1, holder2));
        when(dividendRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.distribute(contractId, request);

        assertThat(result.recipientCount()).isEqualTo(2);
        assertThat(result.grossRentalIncomeUsd()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        // Total distributed should equal gross income
        assertThat(result.totalDistributedUsd()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        verify(dividendRepository, times(2)).save(any(DividendPayment.class));
        verify(dividendDistributedPublisher).publishDividendDistributed(any());
    }

    @Test
    @DisplayName("distribute — throws ConflictException when contract not ACTIVE")
    void distribute_contractNotActive() {
        contract.setStatus(TokenContract.ContractStatus.SUSPENDED);

        var request = new DistributeDividendRequest(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
                BigDecimal.valueOf(1000)
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> service.distribute(contractId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    @DisplayName("distribute — throws ConflictException for duplicate period")
    void distribute_duplicatePeriod() {
        var request = new DistributeDividendRequest(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
                BigDecimal.valueOf(1000)
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(dividendRepository.existsByTokenContractIdAndPeriodStartAndPeriodEnd(
                any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.distribute(contractId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already distributed");
    }

    @Test
    @DisplayName("distribute — throws ConflictException when no active holders")
    void distribute_noHolders() {
        var request = new DistributeDividendRequest(
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29),
                BigDecimal.valueOf(500)
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(dividendRepository.existsByTokenContractIdAndPeriodStartAndPeriodEnd(
                any(), any(), any())).thenReturn(false);
        when(holderRepository.findByTokenContractId(contractId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.distribute(contractId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No active token holders");
    }

    @Test
    @DisplayName("distribute — skips holders with zero balance")
    void distribute_skipsZeroBalanceHolders() {
        holder2.setBalance(0L);  // exited holder

        var request = new DistributeDividendRequest(
                LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31),
                BigDecimal.valueOf(700)
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(dividendRepository.existsByTokenContractIdAndPeriodStartAndPeriodEnd(
                any(), any(), any())).thenReturn(false);
        when(holderRepository.findByTokenContractId(contractId))
                .thenReturn(List.of(holder1, holder2));
        when(dividendRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.distribute(contractId, request);

        // Only holder1 should receive dividends
        assertThat(result.recipientCount()).isEqualTo(1);
    }
}