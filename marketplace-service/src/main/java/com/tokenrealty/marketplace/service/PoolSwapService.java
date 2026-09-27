package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.amm.AmmMath;
import com.tokenrealty.marketplace.client.ComplianceClient;
import com.tokenrealty.marketplace.client.PaymentClient;
import com.tokenrealty.marketplace.client.TokenIssuanceClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.entity.LiquidityPool;
import com.tokenrealty.marketplace.entity.PoolSwap;
import com.tokenrealty.marketplace.repository.LiquidityPoolRepository;
import com.tokenrealty.marketplace.repository.PoolSwapRepository;
import com.tokenrealty.web.exception.ComplianceBlockedException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PoolSwapService {

    private final LiquidityPoolRepository liquidityPoolRepository;
    private final PoolSwapRepository poolSwapRepository;
    private final LiquidityPoolService liquidityPoolService;
    private final ComplianceClient complianceClient;
    private final PaymentClient paymentClient;
    private final TokenIssuanceClient tokenIssuanceClient;

    public SwapQuoteResponse quote(UUID poolId, SwapQuoteRequest request) {
        LiquidityPool pool = liquidityPoolService.getById(poolId);
        return buildQuote(pool, request);
    }

    public Page<PoolSwapResponse> listSwaps(UUID poolId, Pageable pageable) {
        return poolSwapRepository.findByPoolIdOrderByCreatedAtDesc(poolId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public PoolSwapResponse executeSwap(UUID poolId, ExecuteSwapRequest request) {
        LiquidityPool pool = liquidityPoolService.getById(poolId);
        if (pool.getStatus() != LiquidityPool.PoolStatus.ACTIVE) {
            raiseValidation("Pool is paused or closed");
        }
        SwapQuoteResponse quote = buildQuote(pool, new SwapQuoteRequest(
                request.direction(), request.amountIn()));
        assertWalletApproved(request.walletAddress());

        if (request.direction() == PoolSwap.SwapDirection.TOKEN_TO_USDC) {
            long tokensIn = request.amountIn().setScale(0, RoundingMode.DOWN).longValue();
            long balance = tokenIssuanceClient.getHolderBalance(pool.getContractId(), request.walletAddress());
            if (balance < tokensIn) {
                raiseValidation("Insufficient token balance for swap");
            }
        } else {
            String country = assertWalletApproved(request.walletAddress()).countryCode();
            complianceClient.checkInvestment(
                    request.investorId(),
                    country != null ? country : "US",
                    request.amountIn());
        }

        applySwapToPool(pool, quote);
        liquidityPoolRepository.save(pool);

        PoolSwap swap = poolSwapRepository.save(PoolSwap.builder()
                .poolId(pool.getId())
                .contractId(pool.getContractId())
                .swapDirection(request.direction())
                .investorId(request.investorId())
                .walletAddress(request.walletAddress())
                .amountIn(quote.amountIn())
                .amountOut(quote.amountOut())
                .feeUsd(quote.feeUsd())
                .status(PoolSwap.SwapStatus.PENDING)
                .build());

        if (request.direction() == PoolSwap.SwapDirection.USDC_TO_TOKEN) {
            PaymentClient.InitiatePaymentResponse payment = paymentClient.initiateTokenPurchase(
                    swap.getId(),
                    request.investorId(),
                    request.walletAddress(),
                    quote.amountIn());
            swap.setPaymentId(payment.id());
            swap.setStatus(PoolSwap.SwapStatus.COMPLETED);
        } else {
            swap.setStatus(PoolSwap.SwapStatus.COMPLETED);
        }
        return toResponse(poolSwapRepository.save(swap));
    }

    private SwapQuoteResponse buildQuote(LiquidityPool pool, SwapQuoteRequest request) {
        if (pool.getTokenReserve() <= 0 || pool.getUsdcReserve().signum() <= 0) {
            raiseValidation("Pool has no liquidity");
        }
        AmmMath.SwapQuote quote;
        if (request.direction() == PoolSwap.SwapDirection.USDC_TO_TOKEN) {
            quote = AmmMath.quoteUsdcForTokens(
                    pool.getTokenReserve(), pool.getUsdcReserve(), request.amountIn(), pool.getFeeBps());
        } else {
            long tokensIn = request.amountIn().setScale(0, RoundingMode.DOWN).longValue();
            quote = AmmMath.quoteTokensForUsdc(
                    pool.getTokenReserve(), pool.getUsdcReserve(), tokensIn, pool.getFeeBps());
        }
        return SwapQuoteResponse.builder()
                .poolId(pool.getId())
                .direction(request.direction())
                .amountIn(quote.amountIn())
                .amountOut(quote.amountOut())
                .feeUsd(quote.feeUsd())
                .spotPriceAfterUsd(estimateSpotAfter(pool, request, quote))
                .build();
    }

    private static BigDecimal estimateSpotAfter(
            LiquidityPool pool,
            SwapQuoteRequest request,
            AmmMath.SwapQuote quote) {
        if (request.direction() == PoolSwap.SwapDirection.USDC_TO_TOKEN) {
            long newToken = pool.getTokenReserve() - quote.amountOut().longValue();
            BigDecimal newUsdc = pool.getUsdcReserve().add(quote.amountIn());
            return AmmMath.spotPrice(newToken, newUsdc);
        }
        long newToken = pool.getTokenReserve() + request.amountIn().longValue();
        BigDecimal newUsdc = pool.getUsdcReserve().subtract(quote.amountOut());
        return AmmMath.spotPrice(newToken, newUsdc);
    }

    private static void applySwapToPool(LiquidityPool pool, SwapQuoteResponse quote) {
        if (quote.direction() == PoolSwap.SwapDirection.USDC_TO_TOKEN) {
            pool.setUsdcReserve(pool.getUsdcReserve().add(quote.amountIn()));
            pool.setTokenReserve(pool.getTokenReserve() - quote.amountOut().longValue());
        } else {
            pool.setTokenReserve(pool.getTokenReserve() + quote.amountIn().longValue());
            pool.setUsdcReserve(pool.getUsdcReserve().subtract(quote.amountOut()));
        }
    }

    private ComplianceClient.ComplianceCheckResponse assertWalletApproved(String wallet) {
        ComplianceClient.ComplianceCheckResponse response = complianceClient.checkWallet(wallet);
        if (response == null || !response.whitelisted()) {
            throw new ComplianceBlockedException("Wallet is not KYC approved");
        }
        return response;
    }

    private PoolSwapResponse toResponse(PoolSwap swap) {
        return PoolSwapResponse.builder()
                .id(swap.getId())
                .poolId(swap.getPoolId())
                .contractId(swap.getContractId())
                .swapDirection(swap.getSwapDirection())
                .investorId(swap.getInvestorId())
                .amountIn(swap.getAmountIn())
                .amountOut(swap.getAmountOut())
                .feeUsd(swap.getFeeUsd())
                .paymentId(swap.getPaymentId())
                .status(swap.getStatus())
                .createdAt(swap.getCreatedAt())
                .build();
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
