package com.tokenrealty.wallet.crypto;

import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.protobuf.ByteString;
import com.tokenrealty.wallet.config.WalletEncryptionProperties;
import com.tokenrealty.web.exception.ValidationException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Primary
@ConditionalOnProperty(name = "tokenrealty.wallet.encryption.provider", havingValue = "gcp")
@RequiredArgsConstructor
@Slf4j
public class GcpKmsWalletEncryptionService implements KmsWalletEncryptionService {

    private final WalletEncryptionProperties properties;
    private final KeyManagementServiceClient kmsClient;

    @PostConstruct
    void logMode() {
        log.info("Wallet encryption provider=gcp key={}", cryptoKeyName());
    }

    @Override
    public String encrypt(String plainText) {
        try {
            EncryptResponse response = kmsClient.encrypt(cryptoKeyName(),
                    ByteString.copyFromUtf8(plainText));
            return Base64.getEncoder().encodeToString(response.getCiphertext().toByteArray());
        } catch (Exception ex) {
            throw new ValidationException("GCP KMS encrypt failed: " + ex.getMessage());
        }
    }

    @Override
    public String decrypt(String encryptedBase64) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(encryptedBase64);
            DecryptResponse response = kmsClient.decrypt(cryptoKeyName(),
                    ByteString.copyFrom(cipherBytes));
            return response.getPlaintext().toString(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new ValidationException("GCP KMS decrypt failed: " + ex.getMessage());
        }
    }

    private String cryptoKeyName() {
        var gcp = properties.getGcp();
        if (gcp.getProjectId().isBlank() || gcp.getKeyRing().isBlank() || gcp.getCryptoKey().isBlank()) {
            throw new ValidationException(
                    "GCP KMS requires project-id, key-ring, and crypto-key in tokenrealty.wallet.encryption.gcp");
        }
        return CryptoKeyName.of(
                gcp.getProjectId(),
                gcp.getLocation(),
                gcp.getKeyRing(),
                gcp.getCryptoKey()).toString();
    }
}
