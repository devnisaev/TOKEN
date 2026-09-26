package com.tokenrealty.wallet.config;

import com.google.cloud.kms.v1.KeyManagementServiceClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.kms.KmsClient;

import java.io.IOException;

@Configuration
public class KmsClientConfig {

    @Bean
    @ConditionalOnProperty(name = "tokenrealty.wallet.encryption.provider", havingValue = "aws")
    KmsClient awsKmsClient() {
        return KmsClient.create();
    }

    @Bean
    @ConditionalOnProperty(name = "tokenrealty.wallet.encryption.provider", havingValue = "gcp")
    KeyManagementServiceClient gcpKmsClient() throws IOException {
        return KeyManagementServiceClient.create();
    }
}
