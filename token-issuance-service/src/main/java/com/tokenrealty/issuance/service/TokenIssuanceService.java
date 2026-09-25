package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.blockchain.BlockchainConnector;
import com.tokenrealty.issuance.blockchain.ContractDeployer;
import com.tokenrealty.issuance.client.PropertyRegistryClient;
import com.tokenrealty.issuance.dto.IssuanceDtos.*;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.entity.TokenHolder;
import com.tokenrealty.issuance.exception.ConflictException;
import com.tokenrealty.issuance.exception.ResourceNotFoundException;
import com.tokenrealty.issuance.exception.ValidationException;
import com.tokenrealty.issuance.repository.TokenContractRepository;
import com.tokenrealty.issuance.repository.TokenHolderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@Transactional(readOnly = true)
public class TokenIssuanceService {

    private final TokenContractRepository contractRepository;
    private final TokenHolderRepository holderRepository;
    private final ContractDeployer deployer;
    private final BlockchainConnector blockchain;
    private final PropertyRegistryClient registryClient;

    public TokenIssuanceService(TokenContractRepository contractRepository,
                                TokenHolderRepository holderRepository,
                                ContractDeployer deployer,
                                BlockchainConnector blockchain,
                                PropertyRegistryClient registryClient) {
        this.contractRepository = contractRepository;
        this.holderRepository = holderRepository;
        this.deployer = deployer;
        this.blockchain = blockchain;
        this.registryClient = registryClient;
    }

    public Page<TokenContractResponse> findAll(Pageable pageable) {
        return contractRepository.findAll(pageable).map(this::toResponse);
    }

    public Page<TokenContractResponse> findAllActive(Pageable pageable) {
        return contractRepository.findAllActive(pageable).map(this::toResponse);
    }

    public TokenContractResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    public TokenContractResponse findByFlatId(UUID flatId) {
        return contractRepository.findByFlatId(flatId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("TokenContract for flat " + flatId));
    }

    /**
     * Main entry point — validates flat in Property Registry,
     * deploys ERC-1400 contract, registers initial holder (SPV), and
     * calls back Property Registry to update the flat's token info.
     */
    @Transactional
    public TokenContractResponse issueTokens(IssueTokenRequest request) {
        // 1. Check not already issued
        if (contractRepository.existsByFlatId(request.flatId())) {
            throw new ConflictException("Tokens already issued for flat " + request.flatId());
        }

        // 2. Validate flat exists and is AVAILABLE in Property Registry
        PropertyRegistryClient.FlatResponse flat;
        try {
            flat = registryClient.getFlatById(request.flatId());
        } catch (Exception e) {
            throw new ValidationException("Flat " + request.flatId() + " not found in Property Registry: " + e.getMessage());
        }

        if (!"AVAILABLE".equals(flat.status())) {
            throw new ConflictException("Flat " + request.flatId() + " is not AVAILABLE — status: " + flat.status());
        }

        // 3. Validate SPV exists and is KYC verified
        PropertyRegistryClient.SpvResponse spv;
        try {
            spv = registryClient.getSpvByBuilding(request.buildingId());
        } catch (Exception e) {
            throw new ValidationException("SPV not found for building " + request.buildingId());
        }

        if (!Boolean.TRUE.equals(spv.kycVerified())) {
            throw new ConflictException("SPV for building " + request.buildingId() + " is not KYC verified");
        }

        // 4. Create contract record in PENDING state
        TokenContract contract = TokenContract.builder()
                .flatId(request.flatId())
                .buildingId(request.buildingId())
                .spvWalletAddress(request.spvWalletAddress())
                .tokenName(request.tokenName())
                .tokenSymbol(request.tokenSymbol())
                .totalSupply(request.totalSupply())
                .tokenPriceUsd(request.tokenPriceUsd())
                .network("pending")
                .chainId(0L)
                .status(TokenContract.ContractStatus.PENDING)
                .build();
        contract = contractRepository.save(contract);
        log.info("TokenContract created id={} flat={} status=PENDING", contract.getId(), request.flatId());

        // 5. Deploy contract (async in production — sync here for simplicity)
        contract.setStatus(TokenContract.ContractStatus.DEPLOYING);
        contractRepository.save(contract);

        try {
            ContractDeployer.DeploymentResult result = deployer.deployPropertyToken(
                    request.flatId(),
                    request.buildingId(),
                    request.tokenName(),
                    request.tokenSymbol(),
                    request.totalSupply(),
                    request.tokenPriceUsd()
            );

            // 6. Update contract with on-chain details
            contract.setContractAddress(result.contractAddress());
            contract.setDeploymentTxHash(result.txHash());
            contract.setDeployedAt(Instant.now());
            contract.setNetwork(result.network());
            contract.setChainId(result.chainId());
            contract.setStatus(TokenContract.ContractStatus.ACTIVE);
            contract = contractRepository.save(contract);

            log.info("Contract deployed: address={} flat={} tx={}",
                    result.contractAddress(), request.flatId(), result.txHash());

            // 7. Register SPV as initial token holder (holds 100% of supply)
            TokenHolder spvHolder = TokenHolder.builder()
                    .tokenContract(contract)
                    .investorId(UUID.fromString(spv.id().toString()))
                    .walletAddress(request.spvWalletAddress())
                    .balance(request.totalSupply())
                    .balanceUsd(request.tokenPriceUsd().multiply(BigDecimal.valueOf(request.totalSupply())))
                    .ownershipPercentage(BigDecimal.valueOf(100))
                    .kycVerified(true)
                    .whitelistedOnChain(true)
                    .status(TokenHolder.HolderStatus.ACTIVE)
                    .build();
            holderRepository.save(spvHolder);

            // 8. Notify Property Registry — update flat's token info
            try {
                registryClient.setTokenInfo(
                        request.flatId(),
                        result.contractAddress(),
                        request.totalSupply(),
                        request.tokenPriceUsd()
                );
                log.info("Property Registry updated with token info for flat={}", request.flatId());
            } catch (Exception e) {
                // Non-fatal — the contract is deployed, just log the callback failure
                log.warn("Failed to update Property Registry for flat {}: {}", request.flatId(), e.getMessage());
            }

        } catch (Exception e) {
            contract.setStatus(TokenContract.ContractStatus.PENDING);
            contractRepository.save(contract);
            throw new RuntimeException("Token deployment failed: " + e.getMessage(), e);
        }

        return toResponse(contract);
    }

    @Transactional
    public TokenContractResponse enableTransfers(UUID id) {
        TokenContract contract = getOrThrow(id);
        if (contract.getStatus() != TokenContract.ContractStatus.ACTIVE) {
            throw new ConflictException("Contract must be ACTIVE to enable transfers");
        }
        try {
            String txHash = blockchain.sendContractTransaction(
                    contract.getContractAddress(),
                    BlockchainConnector.encodeEnableTransfers(),
                    java.math.BigInteger.ZERO
            );
            log.info("Transfers enabled for contract {} tx={}", contract.getContractAddress(), txHash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to enable transfers on-chain: " + e.getMessage(), e);
        }
        return toResponse(contract);
    }

    @Transactional
    public TokenContractResponse suspendTransfers(UUID id) {
        TokenContract contract = getOrThrow(id);
        try {
            String txHash = blockchain.sendContractTransaction(
                    contract.getContractAddress(),
                    BlockchainConnector.encodeSuspendTransfers(),
                    java.math.BigInteger.ZERO
            );
            contract.setStatus(TokenContract.ContractStatus.SUSPENDED);
            contractRepository.save(contract);
            log.info("Transfers suspended for contract {} tx={}", contract.getContractAddress(), txHash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to suspend transfers on-chain: " + e.getMessage(), e);
        }
        return toResponse(contract);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private TokenContract getOrThrow(UUID id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TokenContract", id));
    }

    private TokenContractResponse toResponse(TokenContract c) {
        return new TokenContractResponse(
                c.getId(), c.getFlatId(), c.getBuildingId(),
                c.getTokenName(), c.getTokenSymbol(), c.getTotalSupply(),
                c.getTokenPriceUsd(), c.getContractAddress(), c.getDeploymentTxHash(),
                c.getDeployedAt(), c.getNetwork(), c.getChainId(), c.getStatus(),
                c.getSpvWalletAddress(),
                c.getHolders().size(),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}