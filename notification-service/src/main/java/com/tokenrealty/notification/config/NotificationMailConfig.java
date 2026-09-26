package com.tokenrealty.notification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class NotificationMailConfig {

    @Bean
    @ConditionalOnProperty(name = "tokenrealty.notification.email.mode", havingValue = "smtp")
    JavaMailSender javaMailSender(
            org.springframework.core.env.Environment environment
    ) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(environment.getProperty("spring.mail.host", "localhost"));
        sender.setPort(environment.getProperty("spring.mail.port", Integer.class, 1025));
        sender.setUsername(environment.getProperty("spring.mail.username", ""));
        sender.setPassword(environment.getProperty("spring.mail.password", ""));

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", environment.getProperty("spring.mail.properties.mail.smtp.auth", "false"));
        props.put("mail.smtp.starttls.enable",
                environment.getProperty("spring.mail.properties.mail.smtp.starttls.enable", "false"));
        return sender;
    }
}
