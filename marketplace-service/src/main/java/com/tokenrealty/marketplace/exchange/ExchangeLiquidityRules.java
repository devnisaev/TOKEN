package com.tokenrealty.marketplace.exchange;

import com.tokenrealty.web.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ExchangeLiquidityRules {

    private ExchangeLiquidityRules() {
    }

    public static void assertClobAllowed(String liquidityTier) {
        if ("TIER_3".equals(liquidityTier)) {
            throw new ValidationException("TIER_3 assets are RFQ-only — use listing-take flow");
        }
    }

    public static BigDecimal minTickUsd(String liquidityTier) {
        return "TIER_2".equals(liquidityTier) ? new BigDecimal("0.05") : new BigDecimal("0.01");
    }

    public static long minLotSize(String liquidityTier) {
        return "TIER_2".equals(liquidityTier) ? 5L : 1L;
    }

    public static BigDecimal navBandPct(String liquidityTier) {
        return "TIER_2".equals(liquidityTier) ? new BigDecimal("15") : new BigDecimal("5");
    }

    public static void validatePriceTick(BigDecimal priceUsd, String liquidityTier) {
        BigDecimal tick = minTickUsd(liquidityTier);
        BigDecimal remainder = priceUsd.remainder(tick);
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            throw new ValidationException("Price must align to min tick " + tick);
        }
    }

    public static void validateLotSize(long quantity, String liquidityTier) {
        long minLot = minLotSize(liquidityTier);
        if (quantity < minLot || quantity % minLot != 0) {
            throw new ValidationException("Quantity must be at least " + minLot + " and a multiple of min lot");
        }
    }

    public static void validateNavBand(BigDecimal limitPrice, BigDecimal navPerToken, String liquidityTier) {
        if (navPerToken == null || navPerToken.signum() <= 0) {
            return;
        }
        BigDecimal bandPct = navBandPct(liquidityTier);
        BigDecimal delta = limitPrice.subtract(navPerToken)
                .divide(navPerToken, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .abs();
        if (delta.compareTo(bandPct) > 0) {
            throw new ValidationException(
                    "Limit price outside NAV band (±" + bandPct + "% of attested NAV " + navPerToken + ")");
        }
    }
}
