package com.tokenrealty.wallet.crypto;

public interface KmsWalletEncryptionService {

    String encrypt(String plainText);

    String decrypt(String encryptedBase64);
}
