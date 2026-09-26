package com.tokenrealty.payment.integration;

import com.tokenrealty.payment.dto.PaymentDtos.ConfirmPaymentRequest;
import com.tokenrealty.payment.dto.PaymentDtos.InitiatePaymentRequest;
import com.tokenrealty.payment.entity.Escrow;
import com.tokenrealty.payment.entity.Payment;
import com.tokenrealty.payment.entity.PaymentCurrency;
import com.tokenrealty.payment.repository.EscrowRepository;
import com.tokenrealty.payment.repository.PaymentRepository;
import com.tokenrealty.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Tag("testcontainers")
@DisplayName("Payment Testcontainers integration test")
class PaymentContainersIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payment_service")
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

    @Autowired PaymentService paymentService;
    @Autowired PaymentRepository paymentRepository;
    @Autowired EscrowRepository escrowRepository;

    @Test
    @DisplayName("initiate and confirm payment persists escrow on PostgreSQL")
    void initiateAndConfirm_persistsOnPostgres() {
        UUID orderId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();

        var initiated = paymentService.initiate(InitiatePaymentRequest.builder()
                .orderId(orderId)
                .payerId(payerId)
                .payerWallet("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266")
                .amount(new BigDecimal("250.00"))
                .currency(PaymentCurrency.USDC)
                .paymentType(Payment.PaymentType.TOKEN_PURCHASE)
                .build(), "tc-" + orderId);

        Payment payment = paymentRepository.findById(initiated.id()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);

        var confirmed = paymentService.confirm(
                payment.getId(), new ConfirmPaymentRequest("0xTcPaymentTx"));

        assertThat(confirmed.status()).isEqualTo(Payment.PaymentStatus.CONFIRMED);
        assertThat(escrowRepository.findByPaymentId(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(Escrow.EscrowStatus.HELD);
    }
}
