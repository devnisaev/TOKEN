package com.tokenrealty.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tokenrealty.payment.blockchain")
public class PaymentBlockchainProperties {

    private boolean enabled;
    private String rpcUrl;
    private Long chainId = 31337L;
    private String operatorPrivateKey;
    private String usdcContractAddress;
    private long gasLimit = 200_000L;
}
