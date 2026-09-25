package com.tokenrealty.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tokenrealty.payment.auto-confirm")
public class PaymentAutoConfirmProperties {

    private boolean enabled;
    private long pollMs = 3000;
}
