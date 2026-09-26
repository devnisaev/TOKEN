package com.tokenrealty.wallet.dto;

import com.tokenrealty.wallet.entity.InvestorWallet.WalletType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WalletDtos {

    private WalletDtos() {
    }

    public record CreateCustodialWalletRequest(
            UUID investorId,
            String label
    ) {
    }

    public record LinkWalletRequest(
            @NotNull UUID investorId,
            @NotBlank @Pattern(regexp = "0x[0-9a-fA-F]{40}") String walletAddress,
            String label,
            boolean primary
    ) {
    }

    public record ConnectSessionRequest(
            UUID investorId
    ) {
    }

    public record ConnectSessionResponse(
            String sessionTopic,
            String uri
    ) {
    }

    public record WalletResponse(
            UUID id,
            UUID investorId,
            String walletAddress,
            WalletType walletType,
            String label,
            boolean primary,
            Instant createdAt
    ) {
    }

    public record SignTransactionRequest(
            @NotBlank String to,
            String data,
            @NotNull BigInteger value,
            @NotNull BigInteger nonce,
            @NotNull BigInteger gasPrice,
            @NotNull BigInteger gasLimit
    ) {
    }

    public record SignTransactionResponse(
            String signedTransactionHex,
            String fromAddress
    ) {
    }

    public record FiatBalanceView(
            String currency,
            BigDecimal available,
            BigDecimal held
    ) {
    }

    public record TokenHoldingView(
            UUID contractId,
            String tokenSymbol,
            String walletAddress,
            long balance
    ) {
    }

    public record AggregateBalanceResponse(
            UUID investorId,
            String primaryWalletAddress,
            List<FiatBalanceView> fiatBalances,
            List<TokenHoldingView> tokenHoldings
    ) {
    }
}
