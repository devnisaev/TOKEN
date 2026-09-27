package com.tokenrealty.marketplace.kafka.in;

import com.tokenrealty.marketplace.kafka.MarketplaceKafkaEventTypes;
import com.tokenrealty.marketplace.kafka.command.NavAttestedCommand;
import com.tokenrealty.marketplace.service.NavCircuitBreakerService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class NavAttestedListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final NavCircuitBreakerService navCircuitBreakerService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.nav-attested}")
    public void onNavAttested(String message) {
        eventConsumer.consume(message, MarketplaceKafkaEventTypes.NAV_ATTESTED,
                "NAV attested processing failed",
                event -> navCircuitBreakerService.onNavAttested(NavAttestedCommand.from(event)));
    }
}
