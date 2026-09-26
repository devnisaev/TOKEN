package com.tokenrealty.wallet.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@ConditionalOnProperty(name = "tokenrealty.wallet.encryption.backend", havingValue = "kms")
@RequiredArgsConstructor
@Slf4j
public class KmsWalletEncryptionDelegate implements KmsWalletEncryptionService {

    private final WalletEncryptionService localDelegate;

    @Override
    public String encrypt(String plainText) {
        log.debug("KMS encryption backend selected — delegating to local AES-GCM (KMS stub)");
        return localDelegate.encrypt(plainText);
    }

    @Override
    public String decrypt(String encryptedBase64) {
        log.debug("KMS decryption backend selected — delegating to local AES-GCM (KMS stub)");
        return localDelegate.decrypt(encryptedBase64);
    }
}
