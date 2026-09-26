package com.tokenrealty.integration.credential;

public interface CredentialEncryptionPort {

    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
