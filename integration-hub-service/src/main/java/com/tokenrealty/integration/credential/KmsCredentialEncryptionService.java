package com.tokenrealty.integration.credential;

import com.tokenrealty.web.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@ConditionalOnProperty(name = "tokenrealty.integration.kms.mode", havingValue = "kms")
public class KmsCredentialEncryptionService implements CredentialEncryptionPort {

    private final String keyId;

    public KmsCredentialEncryptionService(
            @Value("${tokenrealty.integration.kms.key-id:alias/tokenrealty-integration}") String keyId) {
        this.keyId = keyId;
    }

    @Override
    public String encrypt(String plaintext) {
        String encoded = Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
        return "kms:" + keyId + ":" + encoded;
    }

    @Override
    public String decrypt(String ciphertext) {
        if (!ciphertext.startsWith("kms:")) {
            throw new ValidationException("Unsupported KMS credential ciphertext");
        }
        String[] parts = ciphertext.split(":", 3);
        if (parts.length != 3) {
            throw new ValidationException("Invalid KMS credential ciphertext");
        }
        return new String(Base64.getDecoder().decode(parts[2]), StandardCharsets.UTF_8);
    }
}
