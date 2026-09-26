package com.tokenrealty.gateway.bff;

import com.tokenrealty.gateway.client.TokenIssuanceClient;
import com.tokenrealty.gateway.client.WalletClient;
import com.tokenrealty.gateway.dto.BffDtos.EnrichedTokenHoldingView;
import com.tokenrealty.gateway.dto.BffDtos.PortfolioBalanceView;
import com.tokenrealty.gateway.dto.BffDtos.PortfolioBffResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BffPortfolioService {

    private static final int MAX_DIVIDENDS = 20;

    private final WalletClient walletClient;
    private final TokenIssuanceClient issuanceClient;

    public PortfolioBffResponse getPortfolio(UUID investorId) {
        var aggregate = walletClient.getAggregateBalance(investorId);
        var holdings = aggregate.tokenHoldings().stream()
                .map(this::enrichHolding)
                .toList();
        var balance = PortfolioBalanceView.builder()
                .investorId(aggregate.investorId())
                .primaryWalletAddress(aggregate.primaryWalletAddress())
                .fiatBalances(aggregate.fiatBalances())
                .tokenHoldings(holdings)
                .build();
        var dividends = issuanceClient.listInvestorDividends(investorId).stream()
                .sorted(Comparator.comparing(
                                TokenIssuanceClient.DividendPaymentView::createdAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TokenIssuanceClient.DividendPaymentView::periodEnd, Comparator.reverseOrder()))
                .limit(MAX_DIVIDENDS)
                .toList();
        return PortfolioBffResponse.builder()
                .balance(balance)
                .recentDividends(dividends)
                .build();
    }

    private EnrichedTokenHoldingView enrichHolding(WalletClient.TokenHoldingView holding) {
        var contract = issuanceClient.getContract(holding.contractId());
        return EnrichedTokenHoldingView.builder()
                .contractId(holding.contractId())
                .flatId(contract != null ? contract.flatId() : null)
                .tokenSymbol(holding.tokenSymbol())
                .walletAddress(holding.walletAddress())
                .balance(holding.balance())
                .tokenPriceUsd(contract != null ? contract.tokenPriceUsd() : null)
                .build();
    }
}
