package com.tokenrealty.auth.config;

import com.tokenrealty.auth.entity.ServiceAccount;
import com.tokenrealty.auth.entity.User;
import com.tokenrealty.auth.repository.ServiceAccountRepository;
import com.tokenrealty.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ServiceAccountRepository serviceAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${tokenrealty.auth.seed-dev-users:true}")
    private boolean seedDevUsers;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedDevUsers) {
            return;
        }
        seedUser("admin@tokenrealty.com", "admin123", User.UserRole.ADMIN);
        seedUser("manager@tokenrealty.com", "manager123", User.UserRole.PROPERTY_MANAGER);
        seedUser("appraiser@tokenrealty.com", "appraiser123", User.UserRole.APPRAISER);
        seedUser("compliance@tokenrealty.com", "compliance123", User.UserRole.COMPLIANCE);
        seedUser("investor@tokenrealty.com", "investor123", User.UserRole.INVESTOR);
        seedServiceAccount("token-issuance", "issuance-secret", User.UserRole.ADMIN);
        seedServiceAccount("marketplace", "marketplace-secret", User.UserRole.ADMIN);
        seedServiceAccount("rental", "rental-secret", User.UserRole.ADMIN);
        seedServiceAccount("compliance", "compliance-secret", User.UserRole.ADMIN);
        seedServiceAccount("document", "document-secret", User.UserRole.ADMIN);
        seedServiceAccount("wallet", "wallet-secret", User.UserRole.ADMIN);
        seedServiceAccount("blockchain-indexer", "indexer-secret", User.UserRole.ADMIN);
        seedServiceAccount("api-gateway", "gateway-secret", User.UserRole.ADMIN);
    }

    private void seedUser(String email, String password, User.UserRole role) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .enabled(true)
                .build());
        log.info("Seeded dev user {}", email);
    }

    private void seedServiceAccount(String clientId, String secret, User.UserRole role) {
        if (serviceAccountRepository.findByClientIdAndEnabledTrue(clientId).isPresent()) {
            return;
        }
        serviceAccountRepository.save(ServiceAccount.builder()
                .clientId(clientId)
                .secretHash(passwordEncoder.encode(secret))
                .description("Dev service account for " + clientId)
                .role(role)
                .enabled(true)
                .build());
        log.info("Seeded service account {}", clientId);
    }
}
