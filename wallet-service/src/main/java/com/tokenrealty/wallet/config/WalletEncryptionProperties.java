package com.tokenrealty.wallet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tokenrealty.wallet.encryption")
@Data
public class WalletEncryptionProperties {

    /** local (AES-GCM env key) or kms (cloud/stub). */
    private String mode = "local";

    /** stub, aws, or gcp — used when mode=kms. */
    private String provider = "stub";

    private String kmsKeyId = "";

    private Gcp gcp = new Gcp();

    @Data
    public static class Gcp {
        private String projectId = "";
        private String location = "global";
        private String keyRing = "";
        private String cryptoKey = "";
    }
}
