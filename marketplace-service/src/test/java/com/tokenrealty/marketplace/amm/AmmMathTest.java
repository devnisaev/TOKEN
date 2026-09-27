package com.tokenrealty.marketplace.amm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AMM constant-product math")
class AmmMathTest {

    @Test
    @DisplayName("USDC to token quote respects fee and reserves")
    void quoteUsdcForTokens() {
        var quote = AmmMath.quoteUsdcForTokens(
                1000L,
                new BigDecimal("100000.00"),
                new BigDecimal("1000.00"),
                30);
        assertThat(quote.amountOut().longValue()).isGreaterThan(0L);
        assertThat(quote.feeUsd()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Spot price is usdc per token")
    void spotPrice() {
        assertThat(AmmMath.spotPrice(100L, new BigDecimal("10000")))
                .isEqualByComparingTo("100");
    }
}
