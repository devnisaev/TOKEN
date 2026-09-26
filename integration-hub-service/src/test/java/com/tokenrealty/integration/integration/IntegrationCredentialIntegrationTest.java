package com.tokenrealty.integration.integration;

import com.tokenrealty.integration.credential.CredentialEncryptionPort;
import com.tokenrealty.integration.entity.IntegrationType;
import com.tokenrealty.integration.repository.IntegrationCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class IntegrationCredentialIntegrationTest {

    @Autowired IntegrationCredentialRepository repository;
    @Autowired CredentialEncryptionPort encryptionPort;
    @Autowired com.tokenrealty.integration.service.IntegrationCredentialService credentialService;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("rotate credential stores encrypted secret with incremented version")
    void rotateCredential_incrementsVersion() {
        var first = credentialService.rotateCredential(
                IntegrationType.PAYMENT,
                "stripe",
                new com.tokenrealty.integration.dto.IntegrationDtos.RotateCredentialRequest("secret-v1"));
        var second = credentialService.rotateCredential(
                IntegrationType.PAYMENT,
                "stripe",
                new com.tokenrealty.integration.dto.IntegrationDtos.RotateCredentialRequest("secret-v2"));

        assertThat(first.version()).isEqualTo(1);
        assertThat(second.version()).isEqualTo(2);

        var stored = repository.findFirstByIntegrationTypeAndProviderOrderByCredentialVersionDesc(
                        IntegrationType.PAYMENT, "stripe")
                .orElseThrow();
        assertThat(encryptionPort.decrypt(stored.getSecretCiphertext())).isEqualTo("secret-v2");
    }
}
