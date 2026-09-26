package com.tokenrealty.wallet.integration;

import com.tokenrealty.wallet.crypto.KmsWalletEncryptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "tokenrealty.wallet.encryption.mode=kms",
        "tokenrealty.wallet.encryption.provider=stub",
        "tokenrealty.wallet.encryption-key=integration-test-key-min-32-chars!!"
})
@DisplayName("KMS stub wallet encryption integration test")
class KmsStubWalletEncryptionIntegrationTest {

    @Autowired KmsWalletEncryptionService encryptionService;

    @Test
    void encryptDecrypt_roundTripsViaKmsStub() {
        String plain = "0xkmsstubcustodialseed";

        String encrypted = encryptionService.encrypt(plain);
        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(encrypted).isNotBlank().isNotEqualTo(plain);
        assertThat(decrypted).isEqualTo(plain);
    }
}
