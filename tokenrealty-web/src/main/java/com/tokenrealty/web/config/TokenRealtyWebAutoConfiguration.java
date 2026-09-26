package com.tokenrealty.web.config;

import com.tokenrealty.web.exception.TokenRealtyExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(TokenRealtyExceptionHandler.class)
public class TokenRealtyWebAutoConfiguration {
}
