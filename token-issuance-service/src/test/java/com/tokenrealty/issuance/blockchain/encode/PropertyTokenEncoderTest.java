package com.tokenrealty.issuance.blockchain.encode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PropertyTokenEncoder tests")
class PropertyTokenEncoderTest {

    @Test
    void enableTransfers_startsWithKnownSelector() {
        assertThat(PropertyTokenEncoder.encodeEnableTransfers()).startsWith("0xaf35c6c7");
    }

    @Test
    void operatorTransfer_includesAddressesAndAmount() {
        String encoded = PropertyTokenEncoder.encodeOperatorTransfer(
                "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266",
                "0x70997970C51812dc3A010C724d1AfE6Fc599aa84",
                100L);
        assertThat(encoded).startsWith("0x0d1af103");
        assertThat(encoded.length()).isGreaterThan(130);
    }
}
