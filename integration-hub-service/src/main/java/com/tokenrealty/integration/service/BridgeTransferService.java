package com.tokenrealty.integration.service;

import com.tokenrealty.integration.dto.IntegrationDtos.*;
import com.tokenrealty.integration.entity.BridgeTransfer;
import com.tokenrealty.integration.repository.BridgeTransferRepository;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BridgeTransferService {

    private final BridgeTransferRepository bridgeTransferRepository;

    public List<BridgeTransferResponse> listPending() {
        return bridgeTransferRepository.findByStatusOrderByCreatedAtDesc(BridgeTransfer.BridgeStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BridgeTransferResponse create(CreateBridgeTransferRequest request) {
        BridgeTransfer transfer = bridgeTransferRepository.save(BridgeTransfer.builder()
                .sourceContractId(request.sourceContractId())
                .sourceChain(request.sourceChain())
                .targetChain(request.targetChain())
                .investorId(request.investorId())
                .walletAddress(request.walletAddress())
                .tokenAmount(request.tokenAmount())
                .status(BridgeTransfer.BridgeStatus.PENDING)
                .build());
        return toResponse(transfer);
    }

    @Transactional
    public BridgeTransferResponse relay(UUID id) {
        BridgeTransfer transfer = bridgeTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BridgeTransfer", id));
        transfer.setStatus(BridgeTransfer.BridgeStatus.RELAYED);
        transfer.setRelayTxHash("0xbridge-" + id.toString().replace("-", "").substring(0, 16));
        return toResponse(bridgeTransferRepository.save(transfer));
    }

    private BridgeTransferResponse toResponse(BridgeTransfer transfer) {
        return BridgeTransferResponse.builder()
                .id(transfer.getId())
                .sourceContractId(transfer.getSourceContractId())
                .sourceChain(transfer.getSourceChain())
                .targetChain(transfer.getTargetChain())
                .investorId(transfer.getInvestorId())
                .walletAddress(transfer.getWalletAddress())
                .tokenAmount(transfer.getTokenAmount())
                .status(transfer.getStatus())
                .relayTxHash(transfer.getRelayTxHash())
                .createdAt(transfer.getCreatedAt())
                .build();
    }
}
