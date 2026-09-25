package com.tokenrealty.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_accounts", indexes = {
        @Index(name = "idx_service_accounts_client_id", columnList = "client_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceAccount extends BaseEntity {

    @Column(name = "client_id", nullable = false, unique = true, length = 100)
    private String clientId;

    @Column(name = "secret_hash", nullable = false, length = 255)
    private String secretHash;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private User.UserRole role;

    @Column(nullable = false)
    private boolean enabled = true;
}
