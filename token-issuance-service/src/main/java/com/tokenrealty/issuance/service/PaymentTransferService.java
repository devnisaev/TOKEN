package com.tokenrealty.issuance.service;

import com.tokenrealty.issuance.client.MarketplaceClient;
import com.tokenrealty.issuance.dto.IssuanceDtos.TokenTransferResponse;
import com.tokenrealty.issuance.dto.IssuanceDtos.TransferRequest;
import com.tokenrealty.issuance.kafka.command.PaymentConfirmedCommand;
import com.tokenrealty.issuance.kafka.port.TransferCompletedPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTransferService {

    private final MarketplaceClient marketplaceClient;
    private final TokenIssuanceService tokenIssuanceService;
    private final TransferService transferService;
    private final TransferCompletedPublisher transferCompletedPublisher;

    public void executeTransfer(PaymentConfirmedCommand command) {
        MarketplaceClient.TradeSnapshot trade = marketplaceClient.getTradeByOrderId(command.orderId());
        if ("SETTLED".equals(trade.status())) {
            log.debug("Trade already settled for order {}", command.orderId());
            return;
        }
        var contract = tokenIssuanceService.findByFlatId(trade.flatId());
        String fromWallet = "SECONDARY".equals(trade.listingType())
                ? trade.sellerWallet()
                : contract.spvWalletAddress();
        TransferRequest request = new TransferRequest(
                fromWallet,
                trade.buyerWallet(),
                trade.tokenAmount(),
                contract.tokenPriceUsd());
        TokenTransferResponse transfer = transferService.transfer(contract.id(), request);
        transferCompletedPublisher.publishTransferCompleted(new TransferCompletedPublisher.TransferCompletedEvent(
                transfer.id(),
                contract.id(),
                trade.flatId(),
                trade.orderId(),
                trade.id(),
                command.paymentId(),
                fromWallet,
                trade.buyerWallet(),
                trade.tokenAmount(),
                transfer.txHash(),
                transfer.confirmedAt() != null ? transfer.confirmedAt() : Instant.now()));
        log.info("Token transfer {} completed for order {}", transfer.id(), command.orderId());
    }
}
