package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.blockchain.BlockchainConnector;
import com.tokenrealty.issuance.client.ComplianceClient;
import com.tokenrealty.issuance.dto.IssuanceDtos.TransferRequest;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.issuance.entity.TokenTransfer;
import com.tokenrealty.issuance.exception.ComplianceException;
import com.tokenrealty.issuance.repository.TokenContractRepository;
import com.tokenrealty.issuance.repository.TokenHolderRepository;
import com.tokenrealty.issuance.repository.TokenTransferRepository;
import com.tokenrealty.web.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TransferService unit tests")
class TransferServiceTest {

    @Mock TokenContractRepository contractRepository;
    @Mock TokenHolderRepository holderRepository;
    @Mock TokenTransferRepository transferRepository;
    @Mock ComplianceClient complianceClient;
    @Mock BlockchainConnector blockchain;

    @InjectMocks TransferService service;

    private UUID contractId;
    private TokenContract contract;
    private TokenHolder senderHolder;

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
    }

    @Test
    void transfer_contractNotActive() {
        contract.setStatus(TokenContract.ContractStatus.SUSPENDED);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> service.transfer(contractId,
                new TransferRequest("0xSender", "0xRecipient", 100L, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not ACTIVE");
    }

    @Test
    void transfer_senderNotWhitelisted() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(complianceClient.isWalletApproved("0xSender")).thenReturn(false);

        assertThatThrownBy(() -> service.transfer(contractId,
                new TransferRequest("0xSender", "0xRecipient", 100L, null)))
                .isInstanceOf(ComplianceException.class)
                .hasMessageContaining("not KYC approved");
    }

    @Test
    void transfer_recipientNotApproved() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(complianceClient.isWalletApproved("0xSender")).thenReturn(true);
        when(complianceClient.isWalletApproved("0xRecipient")).thenReturn(false);

        assertThatThrownBy(() -> service.transfer(contractId,
                new TransferRequest("0xSender", "0xRecipient", 100L, null)))
                .isInstanceOf(ComplianceException.class)
                .hasMessageContaining("not KYC approved");
    }

    @Test
    void transfer_insufficientBalance() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(complianceClient.isWalletApproved(any())).thenReturn(true);
        when(holderRepository.findByTokenContractIdAndWalletAddress(contractId, "0xSender"))
                .thenReturn(Optional.of(senderHolder));

        assertThatThrownBy(() -> service.transfer(contractId,
                new TransferRequest("0xSender", "0xRecipient", 600L, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void transfer_success() throws Exception {
        var mockReceipt = mock(TransactionReceipt.class);
        when(mockReceipt.getBlockNumber()).thenReturn(java.math.BigInteger.valueOf(99));
        when(mockReceipt.getStatus()).thenReturn("0x1");

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(complianceClient.isWalletApproved(any())).thenReturn(true);
        when(holderRepository.findByTokenContractIdAndWalletAddress(contractId, "0xSender"))
                .thenReturn(Optional.of(senderHolder));
        when(transferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(blockchain.sendContractTransaction(any(), any(), any())).thenReturn("0xTxHash");
        when(blockchain.waitForReceipt("0xTxHash")).thenReturn(mockReceipt);
        when(blockchain.isTransactionSuccessful(mockReceipt)).thenReturn(true);
        when(holderRepository.findByTokenContractIdAndWalletAddress(contractId, "0xRecipient"))
                .thenReturn(Optional.empty());
        when(complianceClient.checkWallet("0xRecipient"))
                .thenReturn(new com.tokenrealty.issuance.client.ComplianceClient.ComplianceCheckResponse(
                        "0xRecipient", true, "APPROVED", UUID.randomUUID()));
        when(holderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.transfer(contractId,
                new TransferRequest("0xSender", "0xRecipient", 100L, BigDecimal.valueOf(50.00)));

        assertThat(result.status()).isEqualTo(TokenTransfer.TransferStatus.CONFIRMED);
        assertThat(result.txHash()).isEqualTo("0xTxHash");
        assertThat(senderHolder.getBalance()).isEqualTo(400L);
    }
}
