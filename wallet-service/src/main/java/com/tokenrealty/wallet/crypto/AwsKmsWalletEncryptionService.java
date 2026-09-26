package com.tokenrealty.wallet.crypto;

import com.tokenrealty.wallet.config.WalletEncryptionProperties;
import com.tokenrealty.web.exception.ValidationException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Primary
@ConditionalOnProperty(name = "tokenrealty.wallet.encryption.provider", havingValue = "aws")
@RequiredArgsConstructor
@Slf4j
public class AwsKmsWalletEncryptionService implements KmsWalletEncryptionService {

    private final WalletEncryptionProperties properties;
    private final KmsClient kmsClient;

    @PostConstruct
    void logMode() {
        log.info("Wallet encryption provider=aws kmsKeyId={}", properties.getKmsKeyId());
    }

    @Override
    public String encrypt(String plainText) {
        requireKeyId();
        try {
            var response = kmsClient.encrypt(EncryptRequest.builder()
                    .keyId(properties.getKmsKeyId())
                    .plaintext(SdkBytes.fromString(plainText, StandardCharsets.UTF_8))
                    .build());
            return Base64.getEncoder().encodeToString(response.ciphertextBlob().asByteArray());
        } catch (Exception ex) {
            throw new ValidationException("AWS KMS encrypt failed: " + ex.getMessage());
        }
    }

    @Override
    public String decrypt(String encryptedBase64) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(encryptedBase64);
            var response = kmsClient.decrypt(DecryptRequest.builder()
                    .ciphertextBlob(SdkBytes.fromByteArray(cipherBytes))
                    .build());
            return response.plaintext().asString(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new ValidationException("AWS KMS decrypt failed: " + ex.getMessage());
        }
    }

    private void requireKeyId() {
        if (properties.getKmsKeyId() == null || properties.getKmsKeyId().isBlank()) {
            throw new ValidationException("tokenrealty.wallet.encryption.kms-key-id is required for AWS KMS");
        }
    }
}
