package com.tokenrealty.payment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.events.EventEnvelope;
import com.tokenrealty.kafka.consume.KafkaEventConsumer;
import com.tokenrealty.payment.entity.Payout;
import com.tokenrealty.payment.kafka.PaymentKafkaEventTypes;
import com.tokenrealty.payment.kafka.command.DividendDistributedCommand;
import com.tokenrealty.payment.repository.PayoutRepository;
import com.tokenrealty.payment.service.DividendPayoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Dividend distributed Kafka integration test")
class DividendDistributedKafkaIntegrationTest {

    @Autowired DividendPayoutService dividendPayoutService;
    @Autowired PayoutRepository payoutRepository;
    @Autowired KafkaEventConsumer eventConsumer;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("dividend.distributed creates payout per holder")
    void dividendDistributed_createsPayouts() throws Exception {
        UUID contractId = UUID.randomUUID();
        UUID flatId = UUID.randomUUID();
        UUID investor1 = UUID.randomUUID();
        UUID investor2 = UUID.randomUUID();

        ingestDividendDistributed(
                contractId,
                flatId,
                "2025-09",
                List.of(
                        holderPayout(investor1, "0xHolder1", "600.00"),
                        holderPayout(investor2, "0xHolder2", "400.00")),
                UUID.randomUUID());

        List<Payout> payouts = payoutRepository.findAll().stream()
                .filter(p -> p.getReferenceId().equals(contractId))
                .toList();
        assertThat(payouts).hasSize(2);
        assertThat(payouts).allMatch(p -> p.getPurpose() == Payout.PayoutPurpose.DIVIDEND);
        assertThat(payouts.stream().map(Payout::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("duplicate eventId is deduped by KafkaEventConsumer")
    void dividendDistributed_dedupesDuplicateEventId() throws Exception {
        UUID contractId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID investorId = UUID.randomUUID();

        var holders = List.of(holderPayout(investorId, "0xHolder1", "100.00"));
        ingestDividendDistributed(contractId, UUID.randomUUID(), "2025-10", holders, eventId);
        ingestDividendDistributed(contractId, UUID.randomUUID(), "2025-10", holders, eventId);

        assertThat(payoutRepository.findAll().stream()
                .filter(p -> p.getReferenceId().equals(contractId))
                .count()).isEqualTo(1);
    }

    private Map<String, Object> holderPayout(UUID investorId, String wallet, String amount) {
        Map<String, Object> holder = new LinkedHashMap<>();
        holder.put("dividendPaymentId", UUID.randomUUID().toString());
        holder.put("investorId", investorId.toString());
        holder.put("walletAddress", wallet);
        holder.put("amount", amount);
        return holder;
    }

    private void ingestDividendDistributed(
            UUID contractId,
            UUID flatId,
            String period,
            List<Map<String, Object>> holderPayouts,
            UUID eventId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contractId", contractId.toString());
        payload.put("flatId", flatId.toString());
        payload.put("period", period);
        payload.put("holderPayouts", holderPayouts);

        String message = objectMapper.writeValueAsString(EventEnvelope.ofWithEventId(
                eventId,
                PaymentKafkaEventTypes.DIVIDEND_DISTRIBUTED,
                contractId.toString(),
                payload));
        eventConsumer.consume(message, PaymentKafkaEventTypes.DIVIDEND_DISTRIBUTED,
                "Dividend distributed processing failed",
                event -> dividendPayoutService.createHolderPayouts(DividendDistributedCommand.from(event)));
    }
}
