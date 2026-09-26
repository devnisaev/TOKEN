package com.tokenrealty.wallet.crypto;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@ConditionalOnProperty(name = "tokenrealty.wallet.encryption.mode", havingValue = "kms")
@RequiredArgsConstructor
@Slf4j
public class KmsWalletEncryptionDelegate implements KmsWalletEncryptionService {

    private final WalletEncryptionService localDelegate;

    @Value("${tokenrealty.wallet.encryption.kms-key-id:}")
    private String kmsKeyId;

    @PostConstruct
    void logMode() {
        log.info("Wallet encryption mode=kms (stub) kmsKeyId={}", kmsKeyId.isBlank() ? "<unset>" : kmsKeyId);
    }

    @Override
    public String encrypt(String plainText) {
        log.debug("KMS encryption mode selected — delegating to local AES-GCM (KMS stub)");
        return localDelegate.encrypt(plainText);
    }

    @Override
    public String decrypt(String encryptedBase64) {
        log.debug("KMS decryption mode selected — delegating to local AES-GCM (KMS stub)");
        return localDelegate.decrypt(encryptedBase64);
    }
}
