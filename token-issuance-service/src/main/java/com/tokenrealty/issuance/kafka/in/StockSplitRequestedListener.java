package com.tokenrealty.issuance.kafka.in;

import com.tokenrealty.issuance.kafka.IssuanceKafkaEventTypes;
import com.tokenrealty.issuance.kafka.command.StockSplitRequestedCommand;
import com.tokenrealty.issuance.service.StockSplitService;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tokenrealty.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class StockSplitRequestedListener {

    private final KafkaEventConsumer eventConsumer;
    private final StockSplitService stockSplitService;

    @KafkaListener(topics = "${tokenrealty.kafka.topic.stock-split-requested}")
    public void onStockSplitRequested(String message) {
        eventConsumer.consume(message, IssuanceKafkaEventTypes.STOCK_SPLIT_REQUESTED,
                "Stock split requested processing failed",
                event -> stockSplitService.applySplit(StockSplitRequestedCommand.from(event)));
    }
}
