package com.tokenrealty.wallet.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@ConditionalOnProperty(name = "tokenrealty.wallet.encryption.mode", havingValue = "local", matchIfMissing = true)
public class WalletEncryptionService implements KmsWalletEncryptionService {

    private final AesGcmWalletCryptoEngine engine;

    public WalletEncryptionService(@Value("${tokenrealty.wallet.encryption-key}") String encryptionKey) {
        this.engine = new AesGcmWalletCryptoEngine(encryptionKey);
    }

    @Override
    public String encrypt(String plainText) {
        return engine.encrypt(plainText);
    }

    @Override
    public String decrypt(String encryptedBase64) {
        return engine.decrypt(encryptedBase64);
    }
}
