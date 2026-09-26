package com.tokenrealty.integration.credential;

import com.tokenrealty.web.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@ConditionalOnProperty(name = "tokenrealty.integration.kms.mode", havingValue = "local", matchIfMissing = true)
public class LocalAesCredentialEncryptionService implements CredentialEncryptionPort {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public LocalAesCredentialEncryptionService(
            @Value("${tokenrealty.integration.kms.local-key:dev-integration-kms-key-change-me}") String localKey) {
        this.secretKey = deriveKey(localKey);
    }

    @Override
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return "local:" + Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new ValidationException("Failed to encrypt credential");
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        if (!ciphertext.startsWith("local:")) {
            throw new ValidationException("Unsupported credential ciphertext prefix");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(ciphertext.substring("local:".length()));
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[payload.length - GCM_IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(payload, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new ValidationException("Failed to decrypt credential");
        }
    }

    private static SecretKeySpec deriveKey(String localKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(localKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception ex) {
            throw new ValidationException("Failed to derive local KMS key");
        }
    }
}
