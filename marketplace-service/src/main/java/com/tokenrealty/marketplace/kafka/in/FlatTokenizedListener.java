package com.tokenrealty.marketplace.kafka.in;

import com.tokenrealty.marketplace.kafka.MarketplaceKafkaEventTypes;
import com.tokenrealty.marketplace.kafka.command.FlatTokenizedCommand;
import com.tokenrealty.marketplace.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class FlatTokenizedListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final ListingService listingService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.flat-tokenized}")
    public void onFlatTokenized(String message) {
        eventConsumer.consume(message, MarketplaceKafkaEventTypes.FLAT_TOKENIZED,
                "Flat tokenized processing failed",
                event -> listingService.createFromFlatTokenized(FlatTokenizedCommand.from(event)));
    }
}
