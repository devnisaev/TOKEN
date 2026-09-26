package com.tokenrealty.wallet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tokenrealty.wallet.sign-rate-limit")
public class WalletSignRateLimitProperties {

    private boolean enabled = true;
    private int requestsPerMinute = 10;
}
