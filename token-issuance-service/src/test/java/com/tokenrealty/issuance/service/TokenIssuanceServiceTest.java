package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.blockchain.BlockchainConnector;
import com.tokenrealty.issuance.blockchain.ContractDeployer;
import com.tokenrealty.issuance.client.PropertyRegistryClient;
import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.exception.ConflictException;
import com.tokenrealty.issuance.exception.ValidationException;
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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenIssuanceService unit tests")
class TokenIssuanceServiceTest {

    @Mock TokenContractRepository contractRepository;
    @Mock TokenHolderRepository holderRepository;
    @Mock ContractDeployer deployer;
    @Mock BlockchainConnector blockchain;
    @Mock PropertyRegistryClient registryClient;

    @InjectMocks TokenIssuanceService service;

    private UUID flatId;
    private UUID buildingId;
    private IssueTokenRequest request;
    private PropertyRegistryClient.FlatResponse flatResponse;
    private PropertyRegistryClient.SpvResponse spvResponse;

    @BeforeEach
    void setUp() {
        flatId = UUID.randomUUID();
        buildingId = UUID.randomUUID();

        request = new IssueTokenRequest(
                flatId, buildingId,
                "Bishkek City Plaza — Flat 101", "BKCP-101",
                1000L, BigDecimal.valueOf(45.00),
                "0xSPVWallet123"
        );

        flatResponse = new PropertyRegistryClient.FlatResponse(
                flatId, buildingId, "Bishkek City Plaza",
                "101", 1, 42.5, 1, 1,
                "AVAILABLE", null, null, null
        );

        spvResponse = new PropertyRegistryClient.SpvResponse(
                UUID.randomUUID(), buildingId,
                "City Plaza SPV LLC", "KG-SPV-001",
                "0xSPVWallet123", true, "ACTIVE"
        );
    }

    @Test
    @DisplayName("issueTokens — successfully deploys contract and returns response")
    void issueTokens_success() throws Exception {
        var deployResult = new ContractDeployer.DeploymentResult(
                "0xContractAddress123", "0xTxHash456", 42L,
                "0xOperator", "localhost", 31337L
        );

        when(contractRepository.existsByFlatId(flatId)).thenReturn(false);
        when(registryClient.getFlatById(flatId)).thenReturn(flatResponse);
        when(registryClient.getSpvByBuilding(buildingId)).thenReturn(spvResponse);
        when(contractRepository.save(any())).thenAnswer(inv -> {
            TokenContract c = inv.getArgument(0);
            return c;
        });
        when(deployer.deployPropertyToken(any(), any(), any(), any(), any(), any()))
                .thenReturn(deployResult);

        var result = service.issueTokens(request);

        assertThat(result).isNotNull();
        assertThat(result.tokenSymbol()).isEqualTo("BKCP-101");
        verify(deployer).deployPropertyToken(flatId, buildingId,
                "Bishkek City Plaza — Flat 101", "BKCP-101",
                1000L, BigDecimal.valueOf(45.00));
        verify(holderRepository).save(any());
    }

    @Test
    @DisplayName("issueTokens — throws ConflictException when already issued")
    void issueTokens_alreadyIssued() throws Exception {
        when(contractRepository.existsByFlatId(flatId)).thenReturn(true);

        assertThatThrownBy(() -> service.issueTokens(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already issued");

        verify(deployer, never()).deployPropertyToken(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("issueTokens — throws ValidationException when flat not found in registry")
    void issueTokens_flatNotFound() {
        when(contractRepository.existsByFlatId(flatId)).thenReturn(false);
        when(registryClient.getFlatById(flatId)).thenThrow(new RuntimeException("404 Not Found"));

        assertThatThrownBy(() -> service.issueTokens(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not found in Property Registry");
    }

    @Test
    @DisplayName("issueTokens — throws ConflictException when flat is not AVAILABLE")
    void issueTokens_flatNotAvailable() {
        var tokenizedFlat = new PropertyRegistryClient.FlatResponse(
                flatId, buildingId, "Bishkek City Plaza",
                "101", 1, 42.5, 1, 1,
                "TOKENIZED", "0xExistingContract", 1000L, BigDecimal.valueOf(45.00)
        );

        when(contractRepository.existsByFlatId(flatId)).thenReturn(false);
        when(registryClient.getFlatById(flatId)).thenReturn(tokenizedFlat);

        assertThatThrownBy(() -> service.issueTokens(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not AVAILABLE");
    }

    @Test
    @DisplayName("issueTokens — throws ConflictException when SPV KYC not verified")
    void issueTokens_spvNotVerified() {
        var unverifiedSpv = new PropertyRegistryClient.SpvResponse(
                UUID.randomUUID(), buildingId,
                "City Plaza SPV LLC", "KG-SPV-001",
                "0xSPVWallet123", false, "PENDING"
        );

        when(contractRepository.existsByFlatId(flatId)).thenReturn(false);
        when(registryClient.getFlatById(flatId)).thenReturn(flatResponse);
        when(registryClient.getSpvByBuilding(buildingId)).thenReturn(unverifiedSpv);

        assertThatThrownBy(() -> service.issueTokens(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not KYC verified");
    }

    @Test
    @DisplayName("findById — throws ResourceNotFoundException when not found")
    void findById_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(contractRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(unknownId))
                .isInstanceOf(com.tokenrealty.issuance.exception.ResourceNotFoundException.class);
    }
}