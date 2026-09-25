package com.tokenrealty.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tokenrealty.jwt")
public class JwtProperties {

    private String secret;
    private String issuer = "tokenrealty-auth";
    private long accessTokenTtlMinutes = 15;
    private long refreshTokenTtlDays = 7;
}
