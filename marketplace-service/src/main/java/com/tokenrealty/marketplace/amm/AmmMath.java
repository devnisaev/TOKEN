package com.tokenrealty.marketplace.amm;

import com.tokenrealty.web.exception.ValidationException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class AmmMath {

    private static final MathContext MC = new MathContext(18, RoundingMode.HALF_UP);
    private static final int USD_SCALE = 2;
    private static final int TOKEN_SCALE = 8;

    private AmmMath() {
    }

    public static SwapQuote quoteUsdcForTokens(
            long tokenReserve,
            BigDecimal usdcReserve,
            BigDecimal usdcIn,
            int feeBps) {
        if (usdcIn.signum() <= 0) {
            raiseValidation("Swap amount must be positive");
        }
        BigDecimal amountInAfterFee = applyFee(usdcIn, feeBps);
        BigDecimal k = BigDecimal.valueOf(tokenReserve).multiply(usdcReserve, MC);
        BigDecimal newUsdc = usdcReserve.add(amountInAfterFee);
        BigDecimal newToken = k.divide(newUsdc, MC);
        BigDecimal tokenOut = BigDecimal.valueOf(tokenReserve).subtract(newToken);
        if (tokenOut.signum() <= 0) {
            raiseValidation("Insufficient pool liquidity for swap");
        }
        long tokenOutLong = tokenOut.setScale(0, RoundingMode.DOWN).longValue();
        if (tokenOutLong <= 0) {
            raiseValidation("Swap output rounds to zero tokens");
        }
        BigDecimal feeUsd = usdcIn.subtract(amountInAfterFee).setScale(USD_SCALE, RoundingMode.HALF_UP);
        return new SwapQuote(usdcIn, BigDecimal.valueOf(tokenOutLong), feeUsd);
    }

    public static SwapQuote quoteTokensForUsdc(
            long tokenReserve,
            BigDecimal usdcReserve,
            long tokensIn,
            int feeBps) {
        if (tokensIn <= 0) {
            raiseValidation("Swap amount must be positive");
        }
        BigDecimal tokenIn = BigDecimal.valueOf(tokensIn);
        BigDecimal amountInAfterFee = applyFee(tokenIn, feeBps);
        BigDecimal k = BigDecimal.valueOf(tokenReserve).multiply(usdcReserve, MC);
        BigDecimal newToken = BigDecimal.valueOf(tokenReserve).add(amountInAfterFee);
        BigDecimal newUsdc = k.divide(newToken, MC);
        BigDecimal usdcOut = usdcReserve.subtract(newUsdc);
        if (usdcOut.signum() <= 0) {
            raiseValidation("Insufficient pool liquidity for swap");
        }
        BigDecimal feeTokens = tokenIn.subtract(amountInAfterFee);
        BigDecimal feeUsd = feeTokens.multiply(spotPrice(tokenReserve, usdcReserve))
                .setScale(USD_SCALE, RoundingMode.HALF_UP);
        return new SwapQuote(tokenIn, usdcOut.setScale(USD_SCALE, RoundingMode.DOWN), feeUsd);
    }

    public static BigDecimal spotPrice(long tokenReserve, BigDecimal usdcReserve) {
        if (tokenReserve <= 0) {
            return BigDecimal.ZERO;
        }
        return usdcReserve.divide(BigDecimal.valueOf(tokenReserve), TOKEN_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal mintInitialShares(long tokenAmount, BigDecimal usdcAmount) {
        double product = tokenAmount * usdcAmount.doubleValue();
        if (product <= 0) {
            raiseValidation("Initial liquidity must be positive");
        }
        return BigDecimal.valueOf(Math.sqrt(product)).setScale(8, RoundingMode.DOWN);
    }

    public static BigDecimal mintProportionalShares(
            long tokenDeposit,
            BigDecimal usdcDeposit,
            long tokenReserve,
            BigDecimal usdcReserve,
            BigDecimal totalShares) {
        if (tokenReserve <= 0 || usdcReserve.signum() <= 0 || totalShares.signum() <= 0) {
            raiseValidation("Pool must be seeded before proportional deposit");
        }
        BigDecimal fromToken = BigDecimal.valueOf(tokenDeposit)
                .multiply(totalShares)
                .divide(BigDecimal.valueOf(tokenReserve), 8, RoundingMode.DOWN);
        BigDecimal fromUsdc = usdcDeposit
                .multiply(totalShares)
                .divide(usdcReserve, 8, RoundingMode.DOWN);
        return fromToken.min(fromUsdc);
    }

    private static BigDecimal applyFee(BigDecimal amountIn, int feeBps) {
        BigDecimal feeMultiplier = BigDecimal.valueOf(10_000L - feeBps)
                .divide(BigDecimal.valueOf(10_000L), MC);
        return amountIn.multiply(feeMultiplier);
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }

    public record SwapQuote(BigDecimal amountIn, BigDecimal amountOut, BigDecimal feeUsd) {
    }
}
