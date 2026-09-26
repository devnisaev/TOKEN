package com.tokenrealty.marketplace.integration;

import com.tokenrealty.marketplace.client.ComplianceClient;
import com.tokenrealty.marketplace.client.PaymentClient;
import com.tokenrealty.marketplace.client.PropertyRegistryClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.PlaceOrderRequest;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.entity.MarketOrder;
import com.tokenrealty.marketplace.entity.Trade;
import com.tokenrealty.marketplace.kafka.outbox.OutboxEventRepository;
import com.tokenrealty.marketplace.repository.ListingRepository;
import com.tokenrealty.marketplace.repository.MarketOrderRepository;
import com.tokenrealty.marketplace.repository.TradeRepository;
import com.tokenrealty.marketplace.service.OrderService;
import com.tokenrealty.outbox.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Tag("testcontainers")
@TestPropertySource(properties = "tokenrealty.kafka.enabled=true")
@DisplayName("Buy flow Kafka Testcontainers integration test")
class BuyFlowKafkaContainersIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void registerKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired OrderService orderService;
    @Autowired ListingRepository listingRepository;
    @Autowired MarketOrderRepository orderRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired OutboxEventRepository outboxEventRepository;

    @MockitoBean ComplianceClient complianceClient;
    @MockitoBean PaymentClient paymentClient;
    @MockitoBean PropertyRegistryClient propertyRegistryClient;

    private UUID listingId;
    private UUID buyerId;

    @BeforeEach
    void seedListing() {
        outboxEventRepository.deleteAll();
        buyerId = UUID.randomUUID();

        when(complianceClient.isWalletApproved(any())).thenReturn(true);
        when(paymentClient.initiateTokenPurchase(any(), any(), any(), any()))
                .thenAnswer(invocation -> new PaymentClient.InitiatePaymentResponse(
                        UUID.randomUUID(),
                        invocation.getArgument(0),
                        null));

        Listing listing = listingRepository.save(Listing.builder()
                .flatId(UUID.randomUUID())
                .contractId(UUID.randomUUID())
                .listingType(Listing.ListingType.PRIMARY)
                .status(Listing.ListingStatus.ACTIVE)
                .priceUsd(new BigDecimal("10.00"))
                .tokensAvailable(100L)
                .tokensTotal(100L)
                .minInvestmentTokens(5L)
                .title("Kafka container listing")
                .build());
        listingId = listing.getId();
    }

    @Test
    @DisplayName("placeBuyOrder enqueues order.matched outbox row with Kafka enabled")
    void placeBuyOrder_enqueuesOutboxWithKafkaContainer() {
        var orderResponse = orderService.placeBuyOrder(PlaceOrderRequest.builder()
                .listingId(listingId)
                .buyerId(buyerId)
                .buyerWallet("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266")
                .tokenAmount(10L)
                .build());

        MarketOrder order = orderRepository.findById(orderResponse.id()).orElseThrow();
        Trade trade = tradeRepository.findByOrderId(order.getId()).orElseThrow();

        assertThat(order.getStatus()).isEqualTo(MarketOrder.OrderStatus.MATCHED);
        assertThat(trade.getStatus()).isEqualTo(Trade.TradeStatus.PENDING);
        assertThat(outboxEventRepository.findAll())
                .anyMatch(event -> event.getStatus() == OutboxStatus.PENDING
                        && event.getAggregateId().equals(order.getId()));
    }
}
