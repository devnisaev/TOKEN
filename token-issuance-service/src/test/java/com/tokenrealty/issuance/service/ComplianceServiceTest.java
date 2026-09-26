package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.blockchain.BlockchainConnector;
import com.tokenrealty.issuance.config.BlockchainProperties;
import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.entity.ComplianceRecord;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.issuance.repository.ComplianceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComplianceService unit tests")
class ComplianceServiceTest {

    @Mock ComplianceRecordRepository complianceRepository;
    @Mock BlockchainConnector blockchain;
    @Mock BlockchainProperties blockchainProps;

    @InjectMocks ComplianceService service;

    private UUID investorId;
    private UUID recordId;
    private ComplianceRecord record;

    @BeforeEach
    void setUp() {
        investorId = UUID.randomUUID();
        recordId = UUID.randomUUID();

        record = ComplianceRecord.builder()
                .investorId(investorId)
                .walletAddress("0xInvestorWallet123")
                .fullName("Aibek Dzhaksybekov")
                .countryCode("KG")
                .kycProvider("Sumsub")
                .status(ComplianceRecord.ComplianceStatus.PENDING)
                .onChainWhitelisted(false)
                .build();
    }

    @Test
    @DisplayName("register — creates compliance record successfully")
    void register_success() {
        var request = new RegisterComplianceRequest(
                investorId, "0xInvestorWallet123",
                "Aibek Dzhaksybekov", "KG", "Sumsub", "REF-001"
        );

        when(complianceRepository.existsByWalletAddress("0xInvestorWallet123")).thenReturn(false);
        when(complianceRepository.existsByInvestorId(investorId)).thenReturn(false);
        when(complianceRepository.save(any())).thenReturn(record);

        var result = service.register(request);

        assertThat(result).isNotNull();
        verify(complianceRepository).save(any(ComplianceRecord.class));
    }

    @Test
    @DisplayName("register — throws ConflictException when wallet already registered")
    void register_duplicateWallet() {
        var request = new RegisterComplianceRequest(
                investorId, "0xInvestorWallet123",
                "Aibek", "KG", "Sumsub", "REF-001"
        );

        when(complianceRepository.existsByWalletAddress("0xInvestorWallet123")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");

        verify(complianceRepository, never()).save(any());
    }

    @Test
    @DisplayName("verify — sets status to APPROVED and attempts on-chain whitelist")
    void verify_success() throws Exception {
        Instant expiry = Instant.now().plusSeconds(365 * 24 * 3600);
        var verifyRequest = new ComplianceVerifyRequest(investorId, expiry);

        when(complianceRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(blockchainProps.getComplianceRegistryAddress()).thenReturn("0xRegistryAddress");
        when(blockchain.sendContractTransaction(any(), any(), any())).thenReturn("0xTxHash");
        when(complianceRepository.save(any())).thenReturn(record);

        service.verify(recordId, verifyRequest);

        assertThat(record.getStatus()).isEqualTo(ComplianceRecord.ComplianceStatus.APPROVED);
        assertThat(record.getKycVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("verify — still approves even if on-chain call fails")
    void verify_onChainFailure_stillApproves() throws Exception {
        Instant expiry = Instant.now().plusSeconds(365 * 24 * 3600);
        var verifyRequest = new ComplianceVerifyRequest(investorId, expiry);

        when(complianceRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(blockchainProps.getComplianceRegistryAddress()).thenReturn("0xRegistryAddress");
        when(blockchain.sendContractTransaction(any(), any(), any()))
                .thenThrow(new RuntimeException("RPC connection refused"));
        when(complianceRepository.save(any())).thenReturn(record);

        // Should NOT throw — on-chain failure is non-fatal
        assertThatNoException().isThrownBy(() -> service.verify(recordId, verifyRequest));
        assertThat(record.getStatus()).isEqualTo(ComplianceRecord.ComplianceStatus.APPROVED);
    }

    @Test
    @DisplayName("revoke — sets status to REVOKED")
    void revoke_success() throws Exception {
        record.setStatus(ComplianceRecord.ComplianceStatus.APPROVED);
        record.setOnChainWhitelisted(true);

        when(complianceRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(blockchainProps.getComplianceRegistryAddress()).thenReturn("");
        when(complianceRepository.save(any())).thenReturn(record);

        service.revoke(recordId, "KYC expired");

        assertThat(record.getStatus()).isEqualTo(ComplianceRecord.ComplianceStatus.REVOKED);
        assertThat(record.getRejectionReason()).isEqualTo("KYC expired");
        assertThat(record.getOnChainWhitelisted()).isFalse();
    }

    @Test
    @DisplayName("checkWallet — returns false when wallet not registered")
    void checkWallet_notRegistered() {
        when(complianceRepository.findByWalletAddress("0xUnknown")).thenReturn(Optional.empty());

        var result = service.checkWallet("0xUnknown");

        assertThat(result.isWhitelisted()).isFalse();
        assertThat(result.isOnChain()).isFalse();
        assertThat(result.status()).isNull();
    }

    @Test
    @DisplayName("findByInvestor — throws ResourceNotFoundException when not found")
    void findByInvestor_notFound() {
        UUID unknown = UUID.randomUUID();
        when(complianceRepository.findByInvestorId(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByInvestor(unknown))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}