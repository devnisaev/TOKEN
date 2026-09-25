package com.tokenrealty.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Column(name = "wallet_address", length = 66)
    private String walletAddress;

    @Column(nullable = false)
    private boolean enabled = true;

    public enum UserRole {
        ADMIN,
        PROPERTY_MANAGER,
        APPRAISER,
        COMPLIANCE,
        INVESTOR,
        TENANT,
        SERVICE
    }
}
