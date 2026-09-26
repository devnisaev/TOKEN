package com.tokenrealty.marketplace.kafka.in;

import com.tokenrealty.marketplace.kafka.MarketplaceKafkaEventTypes;
import com.tokenrealty.marketplace.kafka.command.BuildingApprovedCommand;
import com.tokenrealty.marketplace.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class BuildingApprovedListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final ListingService listingService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.building-approved}")
    public void onBuildingApproved(String message) {
        eventConsumer.consume(message, MarketplaceKafkaEventTypes.BUILDING_APPROVED,
                "Building approved processing failed",
                event -> listingService.recordBuildingApproved(BuildingApprovedCommand.from(event)));
    }
}
