package com.tokenrealty.compliance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tokenrealty.compliance.sumsub")
@Data
public class SumsubProperties {

    private boolean enabled = false;
    private String apiUrl = "https://api.sumsub.com";
    private String appToken = "";
    private String appSecret = "";
    private String levelName = "basic-kyc-level";
    private String webhookSecret = "";
}
