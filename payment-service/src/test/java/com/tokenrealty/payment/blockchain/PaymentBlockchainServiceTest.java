package com.tokenrealty.payment.blockchain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentBlockchainService unit tests")
class PaymentBlockchainServiceTest {

    @Test
    @DisplayName("toTokenUnits converts USD to 6-decimal USDC")
    void toTokenUnits() {
        assertThat(PaymentBlockchainService.toTokenUnits(new BigDecimal("50.00")))
                .isEqualTo(BigInteger.valueOf(50_000_000L));
    }

    @Test
    @DisplayName("encodeErc20Transfer builds transfer selector")
    void encodeErc20Transfer() {
        String encoded = PaymentBlockchainService.encodeErc20Transfer(
                "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
                BigInteger.valueOf(1_000_000L));
        assertThat(encoded).startsWith("0xa9059cbb");
    }
}
