package com.tokenrealty.compliance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tokenrealty.compliance.onfido")
@Data
public class OnfidoProperties {

    private boolean enabled = false;
    private String apiUrl = "https://api.onfido.com/v3.6";
    private String apiToken = "";
    private String webhookSecret = "";
}
