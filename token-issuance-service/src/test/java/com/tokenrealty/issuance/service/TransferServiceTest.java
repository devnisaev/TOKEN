package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.blockchain.BlockchainConnector;
import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.entity.*;
import com.tokenrealty.issuance.exception.ComplianceException;
import com.tokenrealty.issuance.exception.ConflictException;
import com.tokenrealty.issuance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TransferService unit tests")
class TransferServiceTest {

    @Mock TokenContractRepository contractRepository;
    @Mock TokenHolderRepository holderRepository;
    @Mock TokenTransferRepository transferRepository;
    @Mock ComplianceRecordRepository complianceRepository;
    @Mock BlockchainConnector blockchain;

    @InjectMocks TransferService service;

    private UUID contractId;
    private TokenContract contract;
    private TokenHolder senderHolder;
    private ComplianceRecord senderCompliance;
    private ComplianceRecord recipientCompliance;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();

        contract = TokenContract.builder()
                .tokenName("BKCP-101")
                .tokenSymbol("BKCP-101")
                .totalSupply(1000L)
                .tokenPriceUsd(BigDecimal.valueOf(45.00))
                .contractAddress("0xTokenContract")
                .status(TokenContract.ContractStatus.ACTIVE)
                .flatId(UUID.randomUUID())
                .buildingId(UUID.randomUUID())
                .spvWalletAddress("0xSPV")
                .network("localhost")
                .chainId(31337L)
                .build();

        senderHolder = TokenHolder.builder()
                .tokenContract(contract)
                .investorId(UUID.randomUUID())
                .walletAddress("0xSender")
                .balance(500L)
                .status(TokenHolder.HolderStatus.ACTIVE)
                .build();

        senderCompliance = ComplianceRecord.builder()
                .investorId(senderHolder.getInvestorId())
                .walletAddress("0xSender")
                .status(ComplianceRecord.ComplianceStatus.APPROVED)
                .build();

        recipientCompliance = ComplianceRecord.builder()
                .investorId(UUID.randomUUID())
                .walletAddress("0xRecipient")
                .status(ComplianceRecord.ComplianceStatus.APPROVED)
                .build();
    }

    @Test
    @DisplayName("transfer — throws ConflictException when contract not ACTIVE")
    void transfer_contractNotActive() {
        contract.setStatus(TokenContract.ContractStatus.SUSPENDED);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        var request = new TransferRequest("0xSender", "0xRecipient", 100L, null);

        assertThatThrownBy(() -> service.transfer(contractId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not ACTIVE");
    }

    @Test
    @DisplayName("transfer — throws ComplianceException when sender not whitelisted")
    void transfer_senderNotWhitelisted() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(complianceRepository.findByWalletAddress("0xSender")).thenReturn(Optional.empty());

        var request = new TransferRequest("0xSender", "0xRecipient", 100L, null);

        assertThatThrownBy(() -> service.transfer(contractId, request))
                .isInstanceOf(ComplianceException.class)
                .hasMessageContaining("not registered");
    }

    @Test
    @DisplayName("transfer — throws ComplianceException when recipient KYC not approved")
    void transfer_recipientNotApproved() {
        var pendingRecipient = ComplianceRecord.builder()
                .walletAddress("0xRecipient")
                .status(ComplianceRecord.ComplianceStatus.PENDING)
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(complianceRepository.findByWalletAddress("0xSender"))
                .thenReturn(Optional.of(senderCompliance));
        when(complianceRepository.findByWalletAddress("0xRecipient"))
                .thenReturn(Optional.of(pendingRecipient));

        var request = new TransferRequest("0xSender", "0xRecipient", 100L, null);

        assertThatThrownBy(() -> service.transfer(contractId, request))
                .isInstanceOf(ComplianceException.class)
                .hasMessageContaining("not KYC approved");
    }

    @Test
    @DisplayName("transfer — throws ConflictException when insufficient balance")
    void transfer_insufficientBalance() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(complianceRepository.findByWalletAddress("0xSender"))
                .thenReturn(Optional.of(senderCompliance));
        when(complianceRepository.findByWalletAddress("0xRecipient"))
                .thenReturn(Optional.of(recipientCompliance));
        when(holderRepository.findByTokenContractIdAndWalletAddress(contractId, "0xSender"))
                .thenReturn(Optional.of(senderHolder));

        // Try to transfer more than balance (500)
        var request = new TransferRequest("0xSender", "0xRecipient", 600L, null);

        assertThatThrownBy(() -> service.transfer(contractId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    @DisplayName("transfer — succeeds and updates holder balances on confirmation")
    void transfer_success() throws Exception {
        var mockReceipt = mock(TransactionReceipt.class);
        when(mockReceipt.getBlockNumber()).thenReturn(java.math.BigInteger.valueOf(99));
        when(mockReceipt.getStatus()).thenReturn("0x1");

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(complianceRepository.findByWalletAddress("0xSender"))
                .thenReturn(Optional.of(senderCompliance));
        when(complianceRepository.findByWalletAddress("0xRecipient"))
                .thenReturn(Optional.of(recipientCompliance));
        when(holderRepository.findByTokenContractIdAndWalletAddress(contractId, "0xSender"))
                .thenReturn(Optional.of(senderHolder));
        when(transferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(blockchain.sendContractTransaction(any(), any(), any())).thenReturn("0xTxHash");
        when(blockchain.waitForReceipt("0xTxHash")).thenReturn(mockReceipt);
        when(blockchain.isTransactionSuccessful(mockReceipt)).thenReturn(true);
        when(holderRepository.findByTokenContractIdAndWalletAddress(contractId, "0xRecipient"))
                .thenReturn(Optional.empty());
        when(holderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(complianceRepository.findByWalletAddress("0xRecipient"))
                .thenReturn(Optional.of(recipientCompliance));

        var request = new TransferRequest("0xSender", "0xRecipient", 100L,
                BigDecimal.valueOf(50.00));

        var result = service.transfer(contractId, request);

        assertThat(result.status()).isEqualTo(TokenTransfer.TransferStatus.CONFIRMED);
        assertThat(result.txHash()).isEqualTo("0xTxHash");
        assertThat(senderHolder.getBalance()).isEqualTo(400L);
    }
}