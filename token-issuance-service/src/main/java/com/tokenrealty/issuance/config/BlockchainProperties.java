package com.tokenrealty.issuance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "blockchain")
@Data
public class BlockchainProperties {
    private String network;
    private String rpcUrl;
    private Long chainId;
    private String operatorPrivateKey;
    private Long gasLimit = 500_000L;
    private String complianceRegistryAddress;
}