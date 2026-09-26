package com.tokenrealty.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

import com.tokenrealty.jpa.entity.BaseEntity;
@Entity
@Table(name = "wallet_balances", indexes = {
        @Index(name = "uk_wallet_balances_investor_currency",
                columnList = "investor_id, currency", unique = true)
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WalletBalance extends BaseEntity {

    @Column(name = "investor_id", nullable = false)
    private UUID investorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentCurrency currency;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "held_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal heldBalance;
}
