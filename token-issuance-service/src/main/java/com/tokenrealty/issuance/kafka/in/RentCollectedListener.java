package com.tokenrealty.issuance.kafka.in;

import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.command.RentCollectedCommand;
import com.tokenrealty.issuance.service.RentDividendService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@ConditionalOnProperty(name = "tokenrealty.dividend.rent-collected-listener-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RentCollectedListener {

    private final com.tokenrealty.kafka.consume.KafkaEventConsumer eventConsumer;
    private final RentDividendService rentDividendService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.rent-collected}")
    public void onRentCollected(String message) {
        eventConsumer.consume(message, IssuanceKafkaEventTypes.RENT_COLLECTED,
                "Rent collected processing failed",
                event -> rentDividendService.distributeFromRent(RentCollectedCommand.from(event)));
    }
}
