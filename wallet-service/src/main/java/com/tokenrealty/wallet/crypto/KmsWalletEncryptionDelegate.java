package com.tokenrealty.wallet.crypto;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@ConditionalOnProperty(name = "tokenrealty.wallet.encryption.mode", havingValue = "kms")
@ConditionalOnProperty(name = "tokenrealty.wallet.encryption.provider", havingValue = "stub", matchIfMissing = true)
@Slf4j
public class KmsWalletEncryptionDelegate implements KmsWalletEncryptionService {

    private final AesGcmWalletCryptoEngine engine;

    @Value("${tokenrealty.wallet.encryption.kms-key-id:}")
    private String kmsKeyId;

    public KmsWalletEncryptionDelegate(@Value("${tokenrealty.wallet.encryption-key}") String encryptionKey) {
        this.engine = new AesGcmWalletCryptoEngine(encryptionKey);
    }

    @PostConstruct
    void logMode() {
        log.info("Wallet encryption mode=kms (stub) kmsKeyId={}", kmsKeyId.isBlank() ? "<unset>" : kmsKeyId);
    }

    @Override
    public String encrypt(String plainText) {
        log.debug("KMS encryption mode selected — delegating to local AES-GCM (KMS stub)");
        return engine.encrypt(plainText);
    }

    @Override
    public String decrypt(String encryptedBase64) {
        log.debug("KMS decryption mode selected — delegating to local AES-GCM (KMS stub)");
        return engine.decrypt(encryptedBase64);
    }
}
