package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.dto.IssuanceDtos.IssueIndexTokenRequest;
import com.tokenrealty.issuance.dto.IssuanceDtos.TokenContractResponse;
import com.tokenrealty.issuance.entity.TokenContract;
import com.tokenrealty.issuance.repository.TokenContractRepository;
import com.tokenrealty.web.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IndexTokenIssuanceService {

    private final TokenContractRepository contractRepository;

    @Transactional
    public TokenContractResponse issueIndexToken(IssueIndexTokenRequest request) {
        UUID syntheticFlatId = request.indexDefinitionId();
        if (contractRepository.existsByFlatId(syntheticFlatId)) {
            throw new ConflictException("Index token already issued for index " + request.indexDefinitionId());
        }
        TokenContract contract = contractRepository.save(TokenContract.builder()
                .flatId(syntheticFlatId)
                .buildingId(syntheticFlatId)
                .spvWalletAddress(request.spvWalletAddress())
                .tokenName("INDEX: " + request.tokenName())
                .tokenSymbol(request.tokenSymbol())
                .totalSupply(request.totalSupply())
                .tokenPriceUsd(request.tokenPriceUsd())
                .network("index-wrapper")
                .chainId(0L)
                .status(TokenContract.ContractStatus.ACTIVE)
                .build());
        return new TokenContractResponse(
                contract.getId(),
                contract.getFlatId(),
                contract.getBuildingId(),
                contract.getTokenName(),
                contract.getTokenSymbol(),
                contract.getTotalSupply(),
                contract.getTokenPriceUsd(),
                contract.getContractAddress(),
                contract.getDeploymentTxHash(),
                contract.getDeployedAt(),
                contract.getNetwork(),
                contract.getChainId(),
                contract.getStatus(),
                contract.getSpvWalletAddress(),
                0,
                contract.getCreatedAt(),
                contract.getUpdatedAt());
    }
}
