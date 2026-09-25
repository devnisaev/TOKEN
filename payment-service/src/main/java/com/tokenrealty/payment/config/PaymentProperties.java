package com.tokenrealty.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tokenrealty.payment")
public class PaymentProperties {

    private String escrowWalletAddress;
}
