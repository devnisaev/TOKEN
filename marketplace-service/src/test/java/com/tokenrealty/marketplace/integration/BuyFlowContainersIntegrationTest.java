package com.tokenrealty.marketplace.integration;

import com.tokenrealty.marketplace.client.ComplianceClient;
import com.tokenrealty.marketplace.client.PaymentClient;
import com.tokenrealty.marketplace.client.PropertyRegistryClient;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.PlaceOrderRequest;
import com.tokenrealty.marketplace.entity.Listing;
import com.tokenrealty.marketplace.entity.MarketOrder;
import com.tokenrealty.marketplace.repository.ListingRepository;
import com.tokenrealty.marketplace.repository.MarketOrderRepository;
import com.tokenrealty.marketplace.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Tag("testcontainers")
@DisplayName("Buy flow Testcontainers integration test")
class BuyFlowContainersIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("marketplace")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired OrderService orderService;
    @Autowired ListingRepository listingRepository;
    @Autowired MarketOrderRepository orderRepository;

    @MockitoBean ComplianceClient complianceClient;
    @MockitoBean PaymentClient paymentClient;
    @MockitoBean PropertyRegistryClient propertyRegistryClient;

    private UUID listingId;

    @BeforeEach
    void setUp() {
        when(complianceClient.isWalletApproved(any())).thenReturn(true);
        when(paymentClient.initiateTokenPurchase(any(), any(), any(), any()))
                .thenReturn(new PaymentClient.InitiatePaymentResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new PaymentClient.EscrowResponse(UUID.randomUUID(), "0xescrow", "HELD")));

        Listing listing = listingRepository.save(Listing.builder()
                .flatId(UUID.randomUUID())
                .contractId(UUID.randomUUID())
                .listingType(Listing.ListingType.PRIMARY)
                .status(Listing.ListingStatus.ACTIVE)
                .priceUsd(new BigDecimal("100.00"))
                .tokensAvailable(1000L)
                .tokensTotal(1000L)
                .minInvestmentTokens(1L)
                .title("Demo listing")
                .build());
        listingId = listing.getId();
    }

    @Test
    @DisplayName("placeBuyOrder persists matched order on PostgreSQL")
    void placeBuyOrder_persistsOnPostgres() {
        UUID buyerId = UUID.randomUUID();
        var order = orderService.placeBuyOrder(new PlaceOrderRequest(
                listingId,
                buyerId,
                "0xbuyer",
                10L));

        assertThat(order.status()).isEqualTo(MarketOrder.OrderStatus.MATCHED);
        assertThat(orderRepository.findById(order.id())).isPresent();
    }
}
