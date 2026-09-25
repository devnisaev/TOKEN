package com.tokenrealty.auth.repository;

import com.tokenrealty.auth.entity.ServiceAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceAccountRepository extends JpaRepository<ServiceAccount, UUID> {

    Optional<ServiceAccount> findByClientIdAndEnabledTrue(String clientId);
}
