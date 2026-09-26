package com.tokenrealty.gateway.dto;

import com.tokenrealty.gateway.client.PropertyRegistryClient;
import com.tokenrealty.gateway.client.RentalClient;
import com.tokenrealty.gateway.client.TokenIssuanceClient;
import com.tokenrealty.gateway.client.WalletClient;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class BffDtos {

    private BffDtos() {
    }

    @Builder
    public record FlatDetailResponse(
            UUID flatId,
            UUID buildingId,
            String buildingName,
            String flatNumber,
            Integer floor,
            Double areaSqm,
            String status,
            TokenContractSummary tokenContract,
            ListingSummary listing
    ) {
    }

    @Builder
    public record ListingDetailResponse(
            ListingSummary listing,
            UUID flatId,
            String buildingName,
            String flatNumber,
            String flatStatus,
            TokenContractSummary tokenContract
    ) {
    }

    @Builder
    public record TokenContractSummary(
            UUID contractId,
            String contractAddress,
            String tokenSymbol,
            Long totalSupply,
            BigDecimal tokenPriceUsd,
            String status
    ) {
    }

    @Builder
    public record EnrichedTokenHoldingView(
            UUID contractId,
            UUID flatId,
            String tokenSymbol,
            String walletAddress,
            long balance,
            BigDecimal tokenPriceUsd
    ) {
    }

    @Builder
    public record PortfolioBalanceView(
            UUID investorId,
            String primaryWalletAddress,
            List<WalletClient.FiatBalanceView> fiatBalances,
            List<EnrichedTokenHoldingView> tokenHoldings
    ) {
    }

    @Builder
    public record PortfolioBffResponse(
            PortfolioBalanceView balance,
            List<TokenIssuanceClient.DividendPaymentView> recentDividends
    ) {
    }

    @Builder
    public record BuildingBffDetailResponse(
            PropertyRegistryClient.BuildingDetailView building,
            int tokenizedFlatCount,
            int availableFlatCount
    ) {
    }

    @Builder
    public record ListingSummary(
            UUID listingId,
            String listingType,
            String status,
            BigDecimal priceUsd,
            long tokensAvailable,
            long tokensTotal,
            long minInvestmentTokens,
            String title
    ) {
    }

    public record OrderStatusEvent(UUID orderId, String orderStatus, String tradeStatus) {
    }

    @Builder
    public record TenantFlatSummary(
            UUID flatId,
            UUID buildingId,
            String buildingName,
            String flatNumber,
            Integer floor,
            Double areaSqm,
            String status
    ) {
    }

    @Builder
    public record TenantLeaseBffResponse(
            RentalClient.LeaseView lease,
            TenantFlatSummary flat,
            boolean rentDue
    ) {
    }

    @Builder
    public record TenantMaintenanceBffResponse(
            RentalClient.LeaseView lease,
            TenantFlatSummary flat,
            List<RentalClient.MaintenanceTicketView> openTickets
    ) {
    }
}
