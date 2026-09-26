package com.tokenrealty.corporateactions.kafka.in;

import com.tokenrealty.corporateactions.kafka.CorporateActionsKafkaEventTypes;
import com.tokenrealty.corporateactions.kafka.command.DividendDistributedCommand;
import com.tokenrealty.corporateactions.kafka.command.RentCollectedCommand;
import com.tokenrealty.corporateactions.service.CorporateActionsService;
import com.tokenrealty.events.kafka.KafkaJsonEvent;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CorporateActionsEventListener {

    private final KafkaEventConsumer eventConsumer;
    private final CorporateActionsService corporateActionsService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.rent-collected}")
    public void onRentCollected(String message) {
        ingest(message, CorporateActionsKafkaEventTypes.RENT_COLLECTED,
                event -> corporateActionsService.onRentCollected(RentCollectedCommand.from(event)));
    }

    @KafkaListener(topics = "${tokenrealty.kafka.topic.dividend-distributed}")
    public void onDividendDistributed(String message) {
        ingest(message, CorporateActionsKafkaEventTypes.DIVIDEND_DISTRIBUTED,
                event -> corporateActionsService.onDividendDistributed(DividendDistributedCommand.from(event)));
    }

    private void ingest(String message, String eventType, Consumer<KafkaJsonEvent> handler) {
        eventConsumer.consume(message, eventType, "Corporate actions ingest failed", handler);
    }
}
