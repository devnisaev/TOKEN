package com.tokenrealty.wallet.integration;

import com.tokenrealty.wallet.crypto.KmsWalletEncryptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Wallet encryption integration test")
class WalletEncryptionIntegrationTest {

    @Autowired KmsWalletEncryptionService encryptionService;

    @Test
    void encryptDecrypt_roundTrips() {
        String plain = "0xdeadbeefcustodialseed";

        String encrypted = encryptionService.encrypt(plain);
        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(encrypted).isNotBlank().isNotEqualTo(plain);
        assertThat(decrypted).isEqualTo(plain);
    }
}
